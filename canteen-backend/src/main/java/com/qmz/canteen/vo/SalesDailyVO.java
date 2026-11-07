package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesDailyVO {

    private LocalDate saleDate;
    private Integer quantity;
    private BigDecimal amount;
}
