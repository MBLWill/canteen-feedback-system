package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.PayService;
import com.qmz.canteen.vo.CheckoutVO;
import com.qmz.canteen.vo.PayStatusVO;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private static final String PAY_NO_PATTERN = "^[A-Za-z0-9]{1,64}$";

    private final PayService payService;

    /** 为已生成的订单发起支付，返回收银台地址（mock 通道为空且 paid=true） */
    @PostMapping("/orders/{orderId}")
    public Result<CheckoutVO> create(@PathVariable @Positive(message = "订单ID不合法") Long orderId) {
        return Result.success(payService.createForOrder(orderId, UserContext.getUserId()));
    }

    @GetMapping("/{payNo}")
    public Result<PayStatusVO> status(@PathVariable
                                      @Pattern(regexp = PAY_NO_PATTERN, message = "支付单号不合法") String payNo) {
        return Result.success(payService.status(payNo, UserContext.getUserId(), UserContext.isAdmin()));
    }

    @PostMapping("/{payNo}/mock-success")
    public Result<Void> mockSuccess(@PathVariable
                                    @Pattern(regexp = PAY_NO_PATTERN, message = "支付单号不合法") String payNo) {
        payService.mockSuccess(payNo, UserContext.getUserId());
        return Result.success();
    }

    /** 支付宝异步通知：走白名单，靠验签鉴权，返回纯文本 success/failure */
    @PostMapping("/notify/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        String answer = payService.handleAlipayNotify(params, params.toString());
        log.info("支付宝回调处理结果：{} payNo={}", answer, params.get("out_trade_no"));
        return answer;
    }
}
