package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_todo")
public class MerchantTodo {

    public static final String TYPE_INSPECT = "INSPECT";
    public static final String TYPE_BAD_REVIEW = "BAD_REVIEW";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stallId;

    private String type;

    private String title;

    private String content;

    private Long dishId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime handleTime;
}
