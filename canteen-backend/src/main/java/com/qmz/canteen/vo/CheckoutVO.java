package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckoutVO {

    private Long orderId;
    private String orderNo;
    private String payNo;
    private String channel;
    private BigDecimal amount;
    /** 为空表示不需要跳转外部收银台（mock 或已直接支付成功） */
    private String payUrl;
    private LocalDateTime expireTime;
    /** 本次是否已直接支付成功 */
    private Boolean paid;
}
