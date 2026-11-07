package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 美食红黑榜散点：x=单价，y=平均评分，散点大小=被评价次数
 */
@Data
public class DishScatterVO {

    private Long dishId;
    private String dishName;
    private Long stallId;
    private String stallName;
    private String windowNo;

    //** 档口所在区域楼层，如「南区 1 楼」 */
    private String stallZone;

    /** x 轴：菜品单价 */
    private BigDecimal price;
    /** y 轴：平均评分 */
    private Double avgRating;
    /** 散点大小：被评价次数 */
    private Long reviewCount;
    private Double goodRate;

    /**
     * 榜单分类：value-高性价比(黄色涟漪) avoid-避雷(红色涟漪)
     * new-新菜品(蓝色涟漪) normal-普通
     */
    private String tag;
    private String tagLabel;
}
