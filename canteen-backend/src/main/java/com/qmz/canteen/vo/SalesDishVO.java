package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesDishVO {

    private Long dishId;
    private String dishName;
    private Integer quantity;
    private BigDecimal amount;
}
