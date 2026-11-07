package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartVO {

    private List<CartStallGroupVO> groups;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private Integer maxQuantityPerDish;
    private Boolean allowMultiStall;
}
