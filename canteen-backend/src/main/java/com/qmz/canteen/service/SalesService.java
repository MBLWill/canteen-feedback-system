package com.qmz.canteen.service;

import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.SalesOverviewVO;
import com.qmz.canteen.vo.SalesSummaryVO;

import java.util.List;

public interface SalesService {

    SalesOverviewVO overview(Long stallId);

    /** 商家销售指标汇总（菜单总数 / 总收入 / 订单数 / 客户数） */
    SalesSummaryVO summary(Long stallId);

    List<SalesDailyVO> daily(Long stallId, Integer days);
}
