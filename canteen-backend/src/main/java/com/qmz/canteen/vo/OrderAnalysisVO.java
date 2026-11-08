package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商家订单分析（订单总览 + 按时间维度的订单/收入分析）
 */
@Data
public class OrderAnalysisVO {

    /** 进行中订单数（PENDING） */
    private Long pendingCount;
    /** 已完成订单数（DONE） */
    private Long doneCount;
    /** 已取消订单数（CANCELED） */
    private Long canceledCount;
    /** 今日订单数 */
    private Long todayOrderCount;
    /** 今日收入 */
    private BigDecimal todayIncome;

    /** 近 N 天每日订单数（用于订单趋势图） */
    private List<Map<String, Object>> orderTrend;
    /** 近 N 天每日收入（用于收入趋势图） */
    private List<Map<String, Object>> incomeTrend;
}
