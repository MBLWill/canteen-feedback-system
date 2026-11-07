package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dish_sales")
public class DishSale {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stallId;

    private Long dishId;

    private String dishName;

    private LocalDate saleDate;

    private Integer quantity;

    private BigDecimal amount;

    private LocalDateTime createTime;
}
