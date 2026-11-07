package com.qmz.canteen.vo;

import lombok.Data;

import java.util.List;

@Data
public class DishDetailVO {

    private DishVO dish;
    private RatingDistributionVO distribution;
    private List<ReviewVO> latestReviews;
    private List<DishVO> recommendDishes;
}
