package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayStatusVO {

    private String payNo;
    private Long orderId;
    private String orderNo;
    /** CREATED / PAID / CLOSED */
    private String status;
    private String statusLabel;
    private BigDecimal amount;
    private String channel;
    private String tradeNo;
    private LocalDateTime paidTime;
}
