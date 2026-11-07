package com.qmz.canteen.controller;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.OrderQuery;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.OrderService;
import com.qmz.canteen.vo.OrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my")
    public Result<PageResult<OrderVO>> pageMy(@Valid OrderQuery query) {
        return Result.success(orderService.pageMyOrders(query, UserContext.getUserId()));
    }

    @GetMapping("/stall")
    public Result<PageResult<OrderVO>> pageStall(@Valid OrderQuery query) {
        return Result.success(orderService.pageStallOrders(query, UserContext.requireMerchant()));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> pageAll(@Valid OrderQuery query) {
        UserContext.requireAdmin();
        return Result.success(orderService.pageAllOrders(query));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable @Positive(message = "订单ID不合法") Long id) {
        return Result.success(orderService.getOrderDetail(id, UserContext.getUserId(), UserContext.isAdmin()));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable @Positive(message = "订单ID不合法") Long id,
                                     @RequestParam @Size(max = 20, message = "状态取值不合法")
                                     @Pattern(regexp = "^(PENDING|DONE|CANCELED)$",
                                             message = "状态取值不合法") String status) {
        orderService.updateStatus(id, status, UserContext.getUserId(), UserContext.isAdmin(),
                UserContext.merchantStallId());
        return Result.success();
    }
}
