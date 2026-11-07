package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_message")
public class MerchantMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long stallId;

    private String content;

    private Integer status;

    private String reply;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
