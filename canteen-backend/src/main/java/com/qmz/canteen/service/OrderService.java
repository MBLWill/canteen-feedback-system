package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.OrderQuery;
import com.qmz.canteen.dto.OrderRequest;
import com.qmz.canteen.vo.OrderVO;

public interface OrderService {

    Long createOrder(OrderRequest request, Long userId);

    PageResult<OrderVO> pageMyOrders(OrderQuery query, Long userId);

    PageResult<OrderVO> pageAllOrders(OrderQuery query);

    /** 商家查看自己档口全部订单 */
    PageResult<OrderVO> pageStallOrders(OrderQuery query, Long stallId);

    OrderVO getOrderDetail(Long id, Long userId, boolean admin);

    void updateStatus(Long id, String status, Long userId, boolean admin, Long merchantStallId);
}
