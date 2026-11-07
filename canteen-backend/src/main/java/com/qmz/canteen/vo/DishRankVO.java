package com.qmz.canteen.vo;

import lombok.Data;

@Data
public class DishRankVO {

    private Long dishId;
    private String dishName;
    private String image;
    private Long stallId;
    private String stallName;
    private String windowNo;

    //** 档口所在区域楼层，如「南区 1 楼」 */
    private String stallZone;
    private Double avgRating;
    private Long reviewCount;
    private Double goodRate;
}
