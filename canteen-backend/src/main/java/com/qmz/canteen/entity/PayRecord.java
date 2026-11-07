package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_record")
public class PayRecord {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CLOSED = "CLOSED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private Long orderId;

    private Long userId;

    private String channel;

    private BigDecimal amount;

    private String status;

    private String tradeNo;

    private String payUrl;

    private String rawNotify;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
