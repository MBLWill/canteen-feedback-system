package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishSaleMapper;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.service.SalesService;
import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.SalesOverviewVO;
import com.qmz.canteen.vo.SalesSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final DishSaleMapper dishSaleMapper;
    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;

    @Override
    public SalesOverviewVO overview(Long stallId) {
        SalesOverviewVO vo = new SalesOverviewVO();
        vo.setWeekly(dishSaleMapper.selectDishSales(stallId, 7));
        vo.setMonthly(dishSaleMapper.selectDishSales(stallId, 30));
        vo.setDaily(dishSaleMapper.selectDailyIncome(stallId, 14));
        return vo;
    }

    @Override
    public SalesSummaryVO summary(Long stallId) {
        SalesSummaryVO vo = new SalesSummaryVO();

        // 菜单总数：该档口在售菜品数
        long dishCount = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStallId, stallId)
                .eq(Dish::getStatus, 1));
        vo.setDishCount(dishCount);

        // 订单数与收入、客户数：该档口全部订单
        List<DiningOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<DiningOrder>()
                .eq(DiningOrder::getStallId, stallId));
        vo.setOrderCount((long) orders.size());

        BigDecimal income = orders.stream()
                .filter(o -> "PAID".equals(o.getPayStatus()) && o.getTotalAmount() != null)
                .map(DiningOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalIncome(income);

        long customers = orders.stream()
                .map(DiningOrder::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        vo.setCustomerCount(customers);
        return vo;
    }

    @Override
    public List<SalesDailyVO> daily(Long stallId, Integer days) {
        return dishSaleMapper.selectDailyIncome(stallId, days);
    }
}
