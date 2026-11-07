package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 客服检索用的菜品摘要：LEFT JOIN 评价，保证零评价的新菜也能被查到 */
@Data
public class DishBriefVO {

    private Long dishId;
    private String dishName;
    private Long stallId;
    private String stallName;
    private String windowNo;

    //** 档口所在区域楼层，如「南区 1 楼」 */
    private String stallZone;
    private String category;
    private BigDecimal price;
    private String image;
    private Integer featured;
    private Double avgRating;
    private Long reviewCount;
    private Double goodRate;
}
