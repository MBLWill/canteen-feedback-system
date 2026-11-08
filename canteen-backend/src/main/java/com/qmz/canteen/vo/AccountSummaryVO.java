package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 个人中心「我的账户」统计视图（展示型余额）。
 */
@Data
public class AccountSummaryVO {

    /** 账户余额（展示型） */
    private BigDecimal balance;

    /** 总订单数 */
    private Long orderCount;

    /** 总消费（已支付订单金额合计） */
    private BigDecimal totalSpent;

    /** 已完成订单数 */
    private Long completedCount;
}
