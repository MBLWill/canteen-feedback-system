package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.common.enums.OrderStatus;
import com.qmz.canteen.dto.OrderItemRequest;
import com.qmz.canteen.dto.OrderQuery;
import com.qmz.canteen.dto.OrderRequest;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.DiningOrderItem;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.mapper.DiningOrderItemMapper;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.mapper.StallMapper;
import com.qmz.canteen.service.OrderService;
import com.qmz.canteen.vo.OrderItemVO;
import com.qmz.canteen.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int MAX_ADVANCE_DAYS = 7;
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final DiningOrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final StallMapper stallMapper;

    @Override
    public Long createOrder(OrderRequest request, Long userId) {
        Stall stall = stallMapper.selectById(request.getStallId());
        if (stall == null || !Integer.valueOf(1).equals(stall.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该档口不存在或已歇业");
        }
        if (request.getPickupTime().isAfter(LocalDateTime.now().plusDays(MAX_ADVANCE_DAYS))) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "最多只能预约 " + MAX_ADVANCE_DAYS + " 天内的取餐时间");
        }

        List<Long> dishIds = request.getItems().stream().map(OrderItemRequest::getDishId).toList();
        if (dishIds.stream().distinct().count() != dishIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "同一菜品请不要重复选择");
        }
        Map<Long, Dish> dishMap = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                        .in(Dish::getId, dishIds))
                .stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        List<DiningOrderItem> items = new ArrayList<>();
        for (OrderItemRequest item : request.getItems()) {
            Dish dish = dishMap.get(item.getDishId());
            if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "所选菜品不存在或已下架");
            }
            if (!dish.getStallId().equals(request.getStallId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                        "菜品「" + dish.getName() + "」不属于该档口");
            }
            DiningOrderItem entity = new DiningOrderItem();
            entity.setDishId(dish.getId());
            entity.setDishName(dish.getName());
            entity.setPrice(dish.getPrice());
            entity.setQuantity(item.getQuantity());
            items.add(entity);
            total = total.add(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        DiningOrder order = new DiningOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStallId(request.getStallId());
        order.setPickupTime(request.getPickupTime());
        order.setRemark(request.getRemark());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING.name());
        orderMapper.insert(order);

        for (DiningOrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        return order.getId();
    }

    @Override
    public PageResult<OrderVO> pageMyOrders(OrderQuery query, Long userId) {
        return queryPage(query, userId);
    }

    @Override
    public PageResult<OrderVO> pageAllOrders(OrderQuery query) {
        return queryPage(query, null);
    }

    @Override
    public OrderVO getOrderDetail(Long id, Long userId, boolean admin) {
        OrderVO order = orderMapper.selectOrderById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!admin && !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能查看自己的订单");
        }
        fillItems(List.of(order));
        return order;
    }

    @Override
    public PageResult<OrderVO> pageStallOrders(OrderQuery query, Long stallId) {
        query.setStallId(stallId);
        return queryPage(query, null);
    }

    @Override
    public void updateStatus(Long id, String status, Long userId, boolean admin, Long merchantStallId) {
        OrderStatus target = OrderStatus.of(status);
        if (target == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "状态取值不合法");
        }
        DiningOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (merchantStallId != null) {
            // 商家：只能处理自己档口的订单，把待取餐改成已完成/已取消
            if (!merchantStallId.equals(order.getStallId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能处理自己档口的订单");
            }
            if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
                throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "只有待取餐的订单可以处理");
            }
            if (target == OrderStatus.PENDING) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能改回待取餐");
            }
        } else if (!admin) {
            if (!order.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能操作自己的订单");
            }
            if (target != OrderStatus.CANCELED) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "只能取消自己的订单");
            }
            if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
                throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "只有待取餐的订单可以取消");
            }
        } else if (target == OrderStatus.PENDING) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已完成的订单不能改回待取餐");
        }
        DiningOrder update = new DiningOrder();
        update.setId(order.getId());
        update.setStatus(target.name());
        orderMapper.updateById(update);
    }

    private PageResult<OrderVO> queryPage(OrderQuery query, Long userId) {
        Page<OrderVO> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<OrderVO> result = orderMapper.selectOrderPage(page, query, userId);
        fillItems(result.getRecords());
        return PageResult.of(result);
    }

    private void fillItems(List<OrderVO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(OrderVO::getId).toList();
        Map<Long, List<OrderItemVO>> grouped = orderMapper.selectItemsByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemVO::getOrderId));

        // 一次性取出这些用户已评价过的菜品，避免逐单查库
        List<Long> userIds = orders.stream().map(OrderVO::getUserId).distinct().toList();
        Set<String> reviewed = new HashSet<>(orderMapper.selectReviewedPairs(userIds));

        orders.forEach(order -> {
            List<OrderItemVO> items = grouped.getOrDefault(order.getId(), List.of());
            order.setItems(items);
            OrderStatus status = OrderStatus.of(order.getStatus());
            order.setStatusLabel(status == null ? order.getStatus() : status.getLabel());
            order.setPayStatusLabel(payStatusLabel(order.getPayStatus()));

            List<Long> unreviewed = items.stream()
                    .map(OrderItemVO::getDishId)
                    .filter(dishId -> !reviewed.contains(order.getUserId() + ":" + dishId))
                    .distinct()
                    .toList();
            order.setUnreviewedDishIds(unreviewed);
            if (!"PAID".equals(order.getPayStatus()) || "CANCELED".equals(order.getStatus())) {
                order.setReviewStatus("NONE");
                order.setReviewStatusLabel(null);
            } else if (unreviewed.isEmpty()) {
                order.setReviewStatus("REVIEWED");
                order.setReviewStatusLabel("已评价");
            } else {
                order.setReviewStatus("UNREVIEWED");
                order.setReviewStatusLabel("待评价");
            }
        });
    }

    private static String payStatusLabel(String payStatus) {
        if (payStatus == null) {
            return null;
        }
        return switch (payStatus) {
            case "PAID" -> "已支付";
            case "CLOSED" -> "已关闭";
            default -> "待支付";
        };
    }

    private String generateOrderNo() {
        return "CT" + LocalDateTime.now().format(ORDER_NO_TIME)
                + (1000 + ThreadLocalRandom.current().nextInt(9000));
    }
}
