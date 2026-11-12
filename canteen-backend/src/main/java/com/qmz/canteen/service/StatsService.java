package com.qmz.canteen.service;

import com.qmz.canteen.vo.DashboardVO;
import com.qmz.canteen.vo.DishRankVO;
import com.qmz.canteen.vo.DishScatterVO;
import com.qmz.canteen.vo.OverviewVO;
import com.qmz.canteen.vo.StallGoodRateVO;
import com.qmz.canteen.vo.StallPopularityVO;
import com.qmz.canteen.vo.StallScatterVO;
import com.qmz.canteen.vo.TrendVO;

import java.util.List;

public interface StatsService {

    OverviewVO overview();

    List<DishRankVO> dishRanking(Long limit, String period);

    List<StallGoodRateVO> stallGoodRate();

    List<StallPopularityVO> stallTop(Long limit);

    List<TrendVO> ratingTrend(Integer days);

    /** 美食红黑榜：按单价与评分定位菜品，返回前 limit 个散点 */
    List<DishScatterVO> dishScatter(Long limit);

    /** 综合档口评价：学生评分 × 学校卫生分 */
    List<StallScatterVO> stallScatter();

    /** 数据大屏聚合数据：核心指标 + 销量趋势 + 品类/档口/菜品排行 + 评分分布 */
    DashboardVO dashboard(Integer days);
}
