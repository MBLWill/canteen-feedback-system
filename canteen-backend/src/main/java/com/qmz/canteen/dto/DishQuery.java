package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DishQuery {

    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 100000, message = "页码超出范围")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数最多100条")
    private Long size = 12L;

    private String keyword;
    private Long stallId;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minRating;

    /** rating-评分优先 reviews-评价数 priceAsc-价格升 priceDesc-价格降 */
    private String sortBy;
}
