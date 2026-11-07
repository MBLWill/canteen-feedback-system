package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qmz.canteen.dto.OrderQuery;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.vo.OrderItemVO;
import com.qmz.canteen.vo.OrderVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper extends BaseMapper<DiningOrder> {

    IPage<OrderVO> selectOrderPage(IPage<?> page,
                                   @Param("query") OrderQuery query,
                                   @Param("userId") Long userId);

    OrderVO selectOrderById(@Param("id") Long id);

    List<OrderItemVO> selectItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

    /** 批量查用户已评价的 (userId, dishId)，用于订单的评价状态标签 */
    List<String> selectReviewedPairs(@Param("userIds") List<Long> userIds);
}
