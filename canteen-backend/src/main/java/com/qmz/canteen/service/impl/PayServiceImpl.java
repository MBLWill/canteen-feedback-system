package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.config.CommerceProperties;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.PayRecord;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.mapper.PayRecordMapper;
import com.qmz.canteen.payment.PayGateway;
import com.qmz.canteen.payment.PayOrder;
import com.qmz.canteen.payment.PayTradeResult;
import com.qmz.canteen.payment.PayTradeState;
import com.qmz.canteen.service.PayService;
import com.qmz.canteen.vo.CheckoutVO;
import com.qmz.canteen.vo.PayStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private static final String ORDER_PAID = "PAID";
    private static final String ORDER_UNPAID = "UNPAID";
    private static final String ORDER_CLOSED = "CLOSED";

    private final PayRecordMapper payRecordMapper;
    private final OrderMapper orderMapper;
    private final CommerceProperties props;
    private final List<PayGateway> gateways;

    @Override
    @Transactional
    public CheckoutVO createForOrder(Long orderId, Long userId) {
        DiningOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (ORDER_PAID.equals(order.getPayStatus())) {
            throw new BusinessException("该订单已支付");
        }

        PayRecord record = latestOpenRecord(orderId);
        if (record == null) {
            record = createRecord(order);
        }
        return buildCheckout(order, record);
    }

    @Override
    public PayStatusVO status(String payNo, Long userId, boolean admin) {
        PayRecord record = byPayNo(payNo);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "支付单不存在");
        }
        if (!admin && !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能查询自己的支付单");
        }

        // 待支付时向平台主动查单：本机开发收不到异步通知，只能靠这一步确认
        if (PayRecord.STATUS_CREATED.equals(record.getStatus())) {
            syncFromGateway(record);
        }
        return buildStatus(record);
    }

    @Override
    @Transactional
    public void mockSuccess(String payNo, Long userId) {
        PayRecord record = byPayNo(payNo);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "支付单不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能操作自己的支付单");
        }
        if (!"mock".equals(record.getChannel())) {
            throw new BusinessException("只有模拟支付通道可以手动确认");
        }
        if (PayRecord.STATUS_PAID.equals(record.getStatus())) {
            return;
        }
        markPaid(record, "MOCK" + System.currentTimeMillis(), null);
    }

    @Override
    @Transactional
    public String handleAlipayNotify(Map<String, String> params, String rawBody) {
        String payNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        if (payNo == null || tradeStatus == null) {
            log.warn("支付宝回调缺少 out_trade_no/trade_status");
            return "failure";
        }

        PayRecord record = byPayNo(payNo);
        if (record == null) {
            log.warn("支付宝回调对应的支付单不存在：{}", payNo);
            return "failure";
        }

        PayGateway gw = gateway(record.getChannel());
        if (gw == null || !gw.ready()) {
            log.warn("支付宝回调到达但通道不可用：{}", payNo);
            return "failure";
        }
        if (props.getPay().isVerifySignature() && !gw.verifyNotify(params)) {
            log.warn("支付宝回调验签失败：{}", payNo);
            return "failure";
        }

        // 金额必须与本地支付单一致，防止改价回调
        BigDecimal notified = parseAmount(params.get("total_amount"));
        if (notified == null || notified.compareTo(record.getAmount()) != 0) {
            log.warn("支付宝回调金额不一致：payNo={} 本地={} 通知={}", payNo, record.getAmount(), params.get("total_amount"));
            return "failure";
        }

        if (PayRecord.STATUS_PAID.equals(record.getStatus())) {
            return "success";
        }
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            markPaid(record, params.get("trade_no"), props.getPay().isLogRawNotify() ? rawBody : null);
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            close(record);
        }
        return "success";
    }

    @Override
    @Transactional
    public void closeTimeoutOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<DiningOrder> expired = orderMapper.selectList(new LambdaQueryWrapper<DiningOrder>()
                .eq(DiningOrder::getPayStatus, ORDER_UNPAID)
                .isNotNull(DiningOrder::getExpireTime)
                .lt(DiningOrder::getExpireTime, now));
        if (expired.isEmpty()) {
            return;
        }
        for (DiningOrder order : expired) {
            PayRecord record = latestOpenRecord(order.getId());
            if (record != null) {
                // 关单前再查一次，避免用户刚好在超时瞬间付款
                PayTradeResult result = query(gateway(record.getChannel()), record.getPayNo());
                if (result != null && result.state() == PayTradeState.SUCCESS) {
                    markPaid(record, result.tradeNo(), null);
                    continue;
                }
                close(record);
            }
            DiningOrder update = new DiningOrder();
            update.setId(order.getId());
            update.setPayStatus(ORDER_CLOSED);
            orderMapper.updateById(update);
        }
        log.info("超时未支付关单 {} 笔", expired.size());
    }

    private PayRecord createRecord(DiningOrder order) {
        String channel = resolveChannel();
        PayGateway gw = gateway(channel);
        if (gw == null) {
            throw new BusinessException("支付通道不可用");
        }

        String payNo = nextPayNo(order);
        PayOrder payOrder = PayOrder.builder()
                .payNo(payNo)
                .amount(order.getTotalAmount())
                .subject("食堂订单 " + order.getOrderNo())
                .notifyUrl(props.getPay().notifyUrl())
                .returnUrl(trimToNull(props.getPay().getReturnUrl()))
                .build();

        PayRecord record = new PayRecord();
        record.setPayNo(payNo);
        record.setOrderId(order.getId());
        record.setUserId(order.getUserId());
        record.setChannel(channel);
        record.setAmount(order.getTotalAmount());
        record.setStatus(PayRecord.STATUS_CREATED);
        record.setPayUrl(gw.payUrl(payOrder));
        payRecordMapper.insert(record);

        DiningOrder update = new DiningOrder();
        update.setId(order.getId());
        update.setPayNo(payNo);
        update.setPayChannel(channel);
        update.setPayStatus(ORDER_UNPAID);
        if (order.getExpireTime() == null) {
            update.setExpireTime(LocalDateTime.now().plusMinutes(props.getOrder().getPayTimeoutMinutes()));
        }
        orderMapper.updateById(update);
        order.setPayNo(payNo);
        order.setPayChannel(channel);
        order.setPayStatus(ORDER_UNPAID);
        if (update.getExpireTime() != null) {
            order.setExpireTime(update.getExpireTime());
        }

        // mock 通道自动成功，模拟「用户已付款」后的状态流转
        if ("mock".equals(channel) && props.getPay().getMock().isAutoSuccess()) {
            markPaid(record, "MOCK" + System.currentTimeMillis(), null);
        }
        return record;
    }

    private void syncFromGateway(PayRecord record) {
        PayTradeResult result = query(gateway(record.getChannel()), record.getPayNo());
        if (result == null) {
            return;
        }
        if (result.state() == PayTradeState.SUCCESS) {
            markPaid(record, result.tradeNo(), null);
        } else if (result.state() == PayTradeState.CLOSED) {
            close(record);
        }
    }

    private PayTradeResult query(PayGateway gw, String payNo) {
        if (gw == null || !gw.ready()) {
            return null;
        }
        try {
            return gw.query(payNo);
        } catch (Exception e) {
            log.warn("查单失败 payNo={}：{}", payNo, e.getMessage());
            return null;
        }
    }

    private void markPaid(PayRecord record, String tradeNo, String rawNotify) {
        if (PayRecord.STATUS_PAID.equals(record.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        record.setStatus(PayRecord.STATUS_PAID);
        record.setTradeNo(tradeNo);
        record.setPaidTime(now);
        if (rawNotify != null) {
            record.setRawNotify(rawNotify);
        }
        payRecordMapper.updateById(record);

        DiningOrder update = new DiningOrder();
        update.setId(record.getOrderId());
        update.setPayStatus(ORDER_PAID);
        update.setPayNo(record.getPayNo());
        update.setPayChannel(record.getChannel());
        update.setPaidTime(now);
        orderMapper.updateById(update);
        log.info("支付成功 payNo={} orderId={} channel={}", record.getPayNo(), record.getOrderId(), record.getChannel());
    }

    private void close(PayRecord record) {
        record.setStatus(PayRecord.STATUS_CLOSED);
        payRecordMapper.updateById(record);

        DiningOrder update = new DiningOrder();
        update.setId(record.getOrderId());
        update.setPayStatus(ORDER_CLOSED);
        orderMapper.updateById(update);
    }

    /** 配置的通道不可用（密钥缺失/格式错）时按可用通道降级，保证本地开发能跑通 */
    private String resolveChannel() {
        String configured = trimToNull(props.getPay().getChannel());
        if (configured != null) {
            PayGateway gw = gateway(configured);
            if (gw != null && gw.ready()) {
                return gw.channel();
            }
            log.warn("配置的支付通道 {} 不可用，本次降级为 mock", configured);
        }
        PayGateway alipay = gateway("alipay");
        if (alipay != null && alipay.ready()) {
            return "alipay";
        }
        return "mock";
    }

    private PayGateway gateway(String channel) {
        if (channel == null || gateways == null) {
            return null;
        }
        return gateways.stream()
                .filter(g -> g.channel().equalsIgnoreCase(channel))
                .findFirst()
                .orElse(null);
    }

    private String nextPayNo(DiningOrder order) {
        Long used = payRecordMapper.selectCount(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, order.getId()));
        return order.getOrderNo() + String.format("%02d", used + 1);
    }

    private PayRecord latestOpenRecord(Long orderId) {
        return payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, orderId)
                .eq(PayRecord::getStatus, PayRecord.STATUS_CREATED)
                .orderByDesc(PayRecord::getId)
                .last("LIMIT 1"));
    }

    private PayRecord byPayNo(String payNo) {
        return payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getPayNo, payNo)
                .last("LIMIT 1"));
    }

    private CheckoutVO buildCheckout(DiningOrder order, PayRecord record) {
        CheckoutVO vo = new CheckoutVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPayNo(record.getPayNo());
        vo.setChannel(record.getChannel());
        vo.setAmount(record.getAmount());
        vo.setPayUrl(record.getPayUrl());
        vo.setExpireTime(order.getExpireTime());
        vo.setPaid(PayRecord.STATUS_PAID.equals(record.getStatus()));
        return vo;
    }

    private PayStatusVO buildStatus(PayRecord record) {
        PayStatusVO vo = new PayStatusVO();
        vo.setPayNo(record.getPayNo());
        vo.setOrderId(record.getOrderId());
        vo.setStatus(record.getStatus());
        vo.setStatusLabel(label(record.getStatus()));
        vo.setAmount(record.getAmount());
        vo.setChannel(record.getChannel());
        vo.setTradeNo(record.getTradeNo());
        vo.setPaidTime(record.getPaidTime());
        DiningOrder order = orderMapper.selectById(record.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }
        return vo;
    }

    private static String label(String status) {
        return switch (status) {
            case PayRecord.STATUS_PAID -> "已支付";
            case PayRecord.STATUS_CLOSED -> "已关闭";
            default -> "待支付";
        };
    }

    private static BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
