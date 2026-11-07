package com.qmz.canteen.vo;

import lombok.Data;

@Data
public class StallGoodRateVO {

    private Long stallId;
    private String stallName;
    private String windowNo;

    //** 档口所在区域楼层，如「南区 1 楼」 */
    private String stallZone;
    private Long goodCount;
    private Long normalCount;
    private Long badCount;
    private Long total;
    private Double goodRate;
}
