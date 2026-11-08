package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.SalesService;
import com.qmz.canteen.vo.OrderAnalysisVO;
import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.SalesOverviewVO;
import com.qmz.canteen.vo.SalesSummaryVO;
import com.qmz.canteen.vo.TrendDishVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/merchant/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @GetMapping("/overview")
    public Result<SalesOverviewVO> overview() {
        return Result.success(salesService.overview(UserContext.requireMerchant()));
    }

    /** 商家销售指标汇总（菜单总数 / 总收入 / 订单数 / 客户数） */
    @GetMapping("/summary")
    public Result<SalesSummaryVO> summary() {
        return Result.success(salesService.summary(UserContext.requireMerchant()));
    }

    @GetMapping("/daily")
    public Result<List<SalesDailyVO>> daily(@RequestParam(defaultValue = "7")
                                            @Min(value = 1, message = "天数不合法")
                                            @Max(value = 366, message = "天数不合法") Integer days) {
        return Result.success(salesService.daily(UserContext.requireMerchant(), days));
    }

    /** 潜力菜品（趋势项目） */
    @GetMapping("/trend")
    public Result<List<TrendDishVO>> trend() {
        return Result.success(salesService.trend(UserContext.requireMerchant()));
    }

    /** 商家订单分析（订单总览 + 每日订单/收入趋势） */
    @GetMapping("/order-analysis")
    public Result<OrderAnalysisVO> orderAnalysis(@RequestParam(defaultValue = "7")
                                                 @Min(value = 1, message = "天数不合法")
                                                 @Max(value = 366, message = "天数不合法") Integer days) {
        return Result.success(salesService.orderAnalysis(UserContext.requireMerchant(), days));
    }
}
