package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private String userName;
    private Long stallId;
    private String stallName;
    private String windowNo;
    private LocalDateTime pickupTime;
    private String remark;
    private BigDecimal totalAmount;
    private String status;
    private String statusLabel;
    private String payStatus;
    private String payStatusLabel;
    private String payNo;
    private String payChannel;
    private LocalDateTime paidTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    /** NONE-未支付不参与评价 UNREVIEWED-待评价 REVIEWED-已评价 */
    private String reviewStatus;
    private String reviewStatusLabel;
    /** 订单中尚未被该用户评价的菜品，供前端「去评价」跳转 */
    private List<Long> unreviewedDishIds;
    private List<OrderItemVO> items;
}
