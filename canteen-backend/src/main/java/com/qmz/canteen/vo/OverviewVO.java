package com.qmz.canteen.vo;

import lombok.Data;

@Data
public class OverviewVO {

    private Long stallCount;
    private Long dishCount;
    private Long reviewCount;
    private Long feedbackCount;
    private Long pendingFeedbackCount;
    private Long studentCount;
    /** 收藏总数（看板「新增收藏」指标卡用） */
    private Long favoriteCount;
    private Double avgRating;
    private Double goodRate;
    private Long todayReviewCount;
}
