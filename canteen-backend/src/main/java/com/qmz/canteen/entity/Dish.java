package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stallId;

    private String name;

    private BigDecimal price;

    private String category;

    private String image;

    private String description;

    private Integer featured;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
