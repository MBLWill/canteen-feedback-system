package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据大屏聚合返回：一次请求拿到整个看板所需数据，减少前端往返。
 * 销量口径基于 dish_sales 汇总表；时间基准为数据最新日期（演示数据可能不是当天）。
 */
@Data
public class DashboardVO {

    /** 顶部核心指标 */
    private Overview overview;

    /** 销量趋势 */
    private List<TrendPoint> salesTrend;

    /** 各品类销量占比 */
    private List<CategorySales> categorySales;

    /** 档口销量排行 */
    private List<StallSales> stallRank;

    /** 菜品销量排行 */
    private List<DishSales> dishRank;

    /** 评分分布（1-5 星） */
    private List<RatingItem> ratingDist;

    /** 各档口口碑（评分 + 好评率） */
    private List<StallRating> stallRating;

    @Data
    public static class Overview {
        private Long stallCount;
        private Long dishCount;
        private Long reviewCount;
        private Long studentCount;
        /** 累计销量（份） */
        private Long totalSalesQty;
        /** 累计营收（元） */
        private BigDecimal totalSalesAmount;
        /** 数据最新日的销量 */
        private Long latestDayQty;
        /** 数据最新日的营收 */
        private BigDecimal latestDayAmount;
        /** 日均销量 */
        private Long avgDailyQty;
        private Double avgRating;
        private Double goodRate;
        /** 数据截止日期（销量数据最新日） */
        private String dataDate;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Long quantity;
        private BigDecimal amount;
    }

    @Data
    public static class CategorySales {
        private String category;
        private Long quantity;
        private BigDecimal amount;
    }

    @Data
    public static class StallSales {
        private Long stallId;
        private String stallName;
        private String windowNo;
        private String areaLabel;
        private Long quantity;
        private BigDecimal amount;
        private Double avgRating;
    }

    @Data
    public static class DishSales {
        private Long dishId;
        private String dishName;
        private String image;
        private String category;
        private Long stallId;
        private String stallName;
        private BigDecimal price;
        private Long quantity;
        private BigDecimal amount;
    }

    @Data
    public static class RatingItem {
        private Integer star;
        private Long count;
    }

    @Data
    public static class StallRating {
        private Long stallId;
        private String stallName;
        private String windowNo;
        private Double avgRating;
        private Long reviewCount;
        private Double goodRate;
    }
}
