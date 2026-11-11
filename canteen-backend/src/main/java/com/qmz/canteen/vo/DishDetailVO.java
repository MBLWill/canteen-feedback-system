package com.qmz.canteen.vo;

import lombok.Data;

import java.util.List;

@Data
public class DishDetailVO {

    private DishVO dish;
    private RatingDistributionVO distribution;
    private List<ReviewVO> latestReviews;
    private List<DishVO> recommendDishes;
    /** 当前用户是否可评价该菜品（需下过单） */
    private Boolean canReview;
}
