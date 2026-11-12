package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.service.StatsService;
import com.qmz.canteen.vo.DashboardVO;
import com.qmz.canteen.vo.DishRankVO;
import com.qmz.canteen.vo.DishScatterVO;
import com.qmz.canteen.vo.OverviewVO;
import com.qmz.canteen.vo.StallScatterVO;
import com.qmz.canteen.vo.StallGoodRateVO;
import com.qmz.canteen.vo.StallPopularityVO;
import com.qmz.canteen.vo.TrendVO;
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
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.success(statsService.overview());
    }

    @GetMapping("/dish-ranking")
    public Result<List<DishRankVO>> dishRanking(
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "条数范围为1-50")
            @Max(value = 50, message = "条数范围为1-50") Long limit,
            @RequestParam(defaultValue = "all") String period) {
        return Result.success(statsService.dishRanking(limit, period));
    }

    @GetMapping("/stall-good-rate")
    public Result<List<StallGoodRateVO>> stallGoodRate() {
        return Result.success(statsService.stallGoodRate());
    }

    @GetMapping("/stall-top")
    public Result<List<StallPopularityVO>> stallTop(
            @RequestParam(defaultValue = "3") @Min(value = 1, message = "条数范围为1-20")
            @Max(value = 20, message = "条数范围为1-20") Long limit) {
        return Result.success(statsService.stallTop(limit));
    }

    @GetMapping("/rating-trend")
    public Result<List<TrendVO>> ratingTrend(
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "天数范围为1-365")
            @Max(value = 365, message = "天数范围为1-365") Integer days) {
        return Result.success(statsService.ratingTrend(days));
    }

    /** 美食红黑榜散点：x=单价，y=平均评分，散点大小=评价数 */
    @GetMapping("/dish-scatter")
    public Result<List<DishScatterVO>> dishScatter(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "条数不能为负数")
            @Max(value = 500, message = "条数最多500") Long limit) {
        return Result.success(statsService.dishScatter(limit));
    }

    /** 综合档口评价散点：x=学生评分，y=学校卫生分 */
    @GetMapping("/stall-scatter")
    public Result<List<StallScatterVO>> stallScatter() {
        return Result.success(statsService.stallScatter());
    }

    /** 数据大屏：一次返回核心指标、销量趋势、品类/档口/菜品排行、评分分布 */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(
            @RequestParam(defaultValue = "30") @Min(value = 1, message = "天数范围为1-365")
            @Max(value = 365, message = "天数范围为1-365") Integer days) {
        return Result.success(statsService.dashboard(days));
    }
}
