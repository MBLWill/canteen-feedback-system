package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家端销售指标汇总卡片数据。
 */
@Data
public class SalesSummaryVO {

    /** 菜单总数（在售菜品数） */
    private Long dishCount;

    /** 总收入（已支付订单金额合计） */
    private BigDecimal totalIncome;

    /** 订单数 */
    private Long orderCount;

    /** 客户数（下单的去重用户数） */
    private Long customerCount;
}
