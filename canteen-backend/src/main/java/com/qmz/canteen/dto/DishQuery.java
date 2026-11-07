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
    /** south-南区 north-北区，为空表示全部 */
    private String area;
    /** 楼层：1 或 2，为空表示全部 */
    private Integer floor;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minRating;

    /** rating-好评优先 sales-销量优先 comprehensive-综合（按档口窗口号） reviews-评价数 priceAsc/priceDesc/newest */
    private String sortBy;
}
