package com.qmz.canteen.service;

import com.qmz.canteen.dto.CartAddRequest;
import com.qmz.canteen.dto.CheckoutRequest;
import com.qmz.canteen.vo.CartVO;
import com.qmz.canteen.vo.CheckoutVO;

import java.util.List;

public interface CartService {

    CartVO list(Long userId);

    void add(CartAddRequest request, Long userId);

    void updateQuantity(Long itemId, Integer quantity, Long userId);

    void remove(Long itemId, Long userId);

    void clear(Long userId);

    /** 结算购物车中已勾选的菜品：跨档口时自动拆成多笔订单+支付单，清掉已结算的购物车条目 */
    List<CheckoutVO> checkout(CheckoutRequest request, Long userId);
}
