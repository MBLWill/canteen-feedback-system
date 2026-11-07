package com.qmz.canteen.service;

import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.SalesOverviewVO;

import java.util.List;

public interface SalesService {

    SalesOverviewVO overview(Long stallId);

    List<SalesDailyVO> daily(Long stallId, Integer days);
}
