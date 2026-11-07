package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartStallGroupVO {

    private Long stallId;
    private String stallName;
    private String windowNo;
    private List<CartItemVO> items;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
}
