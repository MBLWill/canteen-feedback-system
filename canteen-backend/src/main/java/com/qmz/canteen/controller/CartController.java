package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.CartAddRequest;
import com.qmz.canteen.dto.CartQuantityRequest;
import com.qmz.canteen.dto.CheckoutRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.CartService;
import com.qmz.canteen.vo.CartVO;
import com.qmz.canteen.vo.CheckoutVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Result<CartVO> list() {
        return Result.success(cartService.list(UserContext.getUserId()));
    }

    @PostMapping("/items")
    public Result<Void> add(@RequestBody @Valid CartAddRequest request) {
        cartService.add(request, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/items/{id}")
    public Result<Void> updateQuantity(@PathVariable @Positive(message = "条目ID不合法") Long id,
                                       @RequestBody @Valid CartQuantityRequest request) {
        cartService.updateQuantity(id, request.getQuantity(), UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> remove(@PathVariable @Positive(message = "条目ID不合法") Long id) {
        cartService.remove(id, UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(UserContext.getUserId());
        return Result.success();
    }

    @PostMapping("/checkout")
    public Result<List<CheckoutVO>> checkout(@RequestBody @Valid CheckoutRequest request) {
        return Result.success(cartService.checkout(request, UserContext.getUserId()));
    }
}
