package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DishSale;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishSaleMapper;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.service.SalesService;
import com.qmz.canteen.vo.OrderAnalysisVO;
import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.SalesOverviewVO;
import com.qmz.canteen.vo.SalesSummaryVO;
import com.qmz.canteen.vo.TrendDishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<TrendDishVO> trend(Long stallId) {
        List<TrendDishVO> list = dishSaleMapper.selectTrendDishes(stallId);
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        // 查询近 7 天各菜品每日销量，构造迷你趋势
        List<DishSale> recent = dishSaleMapper.selectList(new LambdaQueryWrapper<DishSale>()
                .eq(DishSale::getStallId, stallId)
                .ge(DishSale::getSaleDate, from));

        Map<Long, Map<LocalDate, Integer>> byDish = new HashMap<>();
        for (DishSale s : recent) {
            byDish.computeIfAbsent(s.getDishId(), k -> new HashMap<>())
                    .merge(s.getSaleDate(), s.getQuantity(), Integer::sum);
        }

        for (TrendDishVO vo : list) {
            // 增长率
            int recentQty = vo.getRecentQty() == null ? 0 : vo.getRecentQty();
            int prevQty = vo.getPreviousQty() == null ? 0 : vo.getPreviousQty();
            if (prevQty > 0) {
                vo.setGrowthRate(BigDecimal.valueOf((recentQty - prevQty) * 100.0 / prevQty)
                        .setScale(1, RoundingMode.HALF_UP));
            } else if (recentQty > 0) {
                vo.setGrowthRate(BigDecimal.valueOf(100.0).setScale(1, RoundingMode.HALF_UP));
            } else {
                vo.setGrowthRate(BigDecimal.ZERO);
            }

            // 近 7 天每日销量（迷你趋势图）
            Map<LocalDate, Integer> m = byDish.get(vo.getDishId());
            List<Integer> trend = new ArrayList<>(7);
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                trend.add(m == null ? 0 : m.getOrDefault(d, 0));
            }
            vo.setTrend(trend);
        }
        return list;
    }

    @Override
    public OrderAnalysisVO orderAnalysis(Long stallId, Integer days) {
        int n = days == null ? 7 : days;
        OrderAnalysisVO vo = new OrderAnalysisVO();

        // 该档口全部订单（含历史，用于状态统计）
        List<DiningOrder> all = orderMapper.selectList(new LambdaQueryWrapper<DiningOrder>()
                .eq(DiningOrder::getStallId, stallId));

        long pending = all.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long done = all.stream().filter(o -> "DONE".equals(o.getStatus())).count();
        long canceled = all.stream().filter(o -> "CANCELED".equals(o.getStatus())).count();
        vo.setPendingCount(pending);
        vo.setDoneCount(done);
        vo.setCanceledCount(canceled);

        // 今日订单数与收入
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        List<DiningOrder> todayOrders = all.stream()
                .filter(o -> o.getCreateTime() != null
                        && !o.getCreateTime().isBefore(todayStart)
                        && o.getCreateTime().isBefore(todayEnd))
                .toList();
        vo.setTodayOrderCount((long) todayOrders.size());
        vo.setTodayIncome(todayOrders.stream()
                .filter(o -> "PAID".equals(o.getPayStatus()) && o.getTotalAmount() != null)
                .map(DiningOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 近 N 天每日订单数与收入
        LocalDate from = today.minusDays(n - 1);
        Map<LocalDate, Long> orderByDay = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> incomeByDay = new LinkedHashMap<>();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            orderByDay.put(d, 0L);
            incomeByDay.put(d, BigDecimal.ZERO);
        }
        for (DiningOrder o : all) {
            if (o.getCreateTime() == null) continue;
            LocalDate d = o.getCreateTime().toLocalDate();
            if (d.isBefore(from)) continue;
            if (orderByDay.containsKey(d)) {
                orderByDay.put(d, orderByDay.get(d) + 1);
                if ("PAID".equals(o.getPayStatus()) && o.getTotalAmount() != null) {
                    incomeByDay.put(d, incomeByDay.get(d).add(o.getTotalAmount()));
                }
            }
        }

        List<Map<String, Object>> orderTrend = new ArrayList<>();
        List<Map<String, Object>> incomeTrend = new ArrayList<>();
        for (LocalDate d : orderByDay.keySet()) {
            Map<String, Object> om = new HashMap<>();
            om.put("date", d.toString());
            om.put("count", orderByDay.get(d));
            orderTrend.add(om);

            Map<String, Object> im = new HashMap<>();
            im.put("date", d.toString());
            im.put("amount", incomeByDay.get(d));
            incomeTrend.add(im);
        }
        vo.setOrderTrend(orderTrend);
        vo.setIncomeTrend(incomeTrend);
        return vo;
    }
}
