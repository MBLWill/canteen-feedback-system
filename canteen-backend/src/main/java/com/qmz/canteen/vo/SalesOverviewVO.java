package com.qmz.canteen.vo;

import lombok.Data;

import java.util.List;

@Data
public class SalesOverviewVO {

    /** 近 7 天各菜销量 */
    private List<SalesDishVO> weekly;
    /** 近 30 天各菜销量 */
    private List<SalesDishVO> monthly;
    /** 近 14 天每日收入 */
    private List<SalesDailyVO> daily;
}
