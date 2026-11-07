package com.qmz.canteen.service.impl;

import com.qmz.canteen.mapper.DishSaleMapper;
import com.qmz.canteen.service.SalesService;
import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.SalesOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final DishSaleMapper dishSaleMapper;

    @Override
    public SalesOverviewVO overview(Long stallId) {
        SalesOverviewVO vo = new SalesOverviewVO();
        vo.setWeekly(dishSaleMapper.selectDishSales(stallId, 7));
        vo.setMonthly(dishSaleMapper.selectDishSales(stallId, 30));
        vo.setDaily(dishSaleMapper.selectDailyIncome(stallId, 14));
        return vo;
    }

    @Override
    public List<SalesDailyVO> daily(Long stallId, Integer days) {
        return dishSaleMapper.selectDailyIncome(stallId, days);
    }
}
