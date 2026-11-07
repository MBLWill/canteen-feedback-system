package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 综合档口评价散点：x=学生评分，y=学校评定卫生分，散点大小=评价数
 */
@Data
public class StallScatterVO {

    private Long stallId;
    private String stallName;
    private String windowNo;

    //** 档口所在区域楼层，如「南区 1 楼」 */
    private String stallZone;
    private String location;
    private String category;
    private String managerName;

    /** x 轴：学生评分均值 */
    private Double avgRating;
    /** y 轴：学校评定的卫生分 */
    private BigDecimal hygieneScore;
    /** 散点大小：评价数 */
    private Long reviewCount;
    private Long dishCount;
    private Double goodRate;

    /** 综合分 = 评分占 60% + 卫生分占 40%，用于优质/劣质榜单排序 */
    private Double composite;

    /**
     * 象限：quality-双高优质(红色涟漪) bad-双低劣质(黑色涟漪) normal-一般
     */
    private String tag;
    private String tagLabel;
}
