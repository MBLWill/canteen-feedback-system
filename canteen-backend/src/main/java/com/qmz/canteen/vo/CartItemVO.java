package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {

    private Long id;
    private Long dishId;
    private String dishName;
    private String image;
    /** 菜品分类，前端用于无图时生成占位底色 */
    private String category;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;

    /** 菜品当前是否在售，1-在售 0-已下架 */
    private Integer dishStatus;
    private Long stallId;
    private String stallName;
    private String windowNo;
}
