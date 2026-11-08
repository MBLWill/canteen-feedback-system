package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 潜力菜品（趋势项目）：近 7 天销量激增的菜品
 */
@Data
public class TrendDishVO {

    private Long dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal price;
    /** 近 7 天销量 */
    private Integer recentQty;
    /** 前 7 天销量 */
    private Integer previousQty;
    /** 增长率（百分比，如 +45.2） */
    private BigDecimal growthRate;
    /** 近 7 天每日销量（用于绘制迷你趋势图） */
    private List<Integer> trend;
}
