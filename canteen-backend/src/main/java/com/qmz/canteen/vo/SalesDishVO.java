package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesDishVO {

    private Long dishId;
    private String dishName;
    /** 菜品图片（畅销榜展示用） */
    private String dishImage;
    private Integer quantity;
    private BigDecimal amount;
}
