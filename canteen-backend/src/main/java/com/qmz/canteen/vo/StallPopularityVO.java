package com.qmz.canteen.vo;

import lombok.Data;

@Data
public class StallPopularityVO {

    private Long stallId;
    private String stallName;
    private String windowNo;

    //** 档口所在区域楼层，如「南区 1 楼」 */
    private String stallZone;
    private String location;
    private String coverImage;
    private Double avgRating;
    private Long reviewCount;
    private Long favoriteCount;
    private Double popularityScore;
    private Integer rank;
}
