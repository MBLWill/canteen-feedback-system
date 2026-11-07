package com.qmz.canteen.service;

import com.qmz.canteen.dto.CartAddRequest;
import com.qmz.canteen.dto.CheckoutRequest;
import com.qmz.canteen.vo.CartVO;
import com.qmz.canteen.vo.CheckoutVO;

public interface CartService {

    CartVO list(Long userId);

    void add(CartAddRequest request, Long userId);

    void updateQuantity(Long itemId, Integer quantity, Long userId);

    void remove(Long itemId, Long userId);

    void clear(Long userId);

    /** 按档口结算：生成订单 + 支付单，清掉已结算的购物车条目 */
    CheckoutVO checkout(CheckoutRequest request, Long userId);
}
