package com.qmz.canteen.mapper;

import com.qmz.canteen.vo.DishBriefVO;
import com.qmz.canteen.vo.DishRankVO;
import com.qmz.canteen.vo.DishScatterVO;
import com.qmz.canteen.vo.StallGoodRateVO;
import com.qmz.canteen.vo.StallScatterVO;
import com.qmz.canteen.vo.StallPopularityVO;
import com.qmz.canteen.vo.TrendVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface StatsMapper {

    Map<String, Object> selectOverview(@Param("goodThreshold") Integer goodThreshold);

    List<DishRankVO> selectDishRanking(@Param("limit") Long limit,
                                       @Param("goodThreshold") Integer goodThreshold,
                                       @Param("period") String period);

    List<StallGoodRateVO> selectStallGoodRate(@Param("goodThreshold") Integer goodThreshold);

    List<StallPopularityVO> selectStallPopularity(@Param("limit") Long limit);

    List<TrendVO> selectRatingTrend(@Param("days") Integer days);

    List<DishScatterVO> selectDishScatter(@Param("goodThreshold") Integer goodThreshold);

    List<StallScatterVO> selectStallScatter(@Param("goodThreshold") Integer goodThreshold);

    Map<String, Object> selectRatingDistribution(@Param("dishId") Long dishId);

    List<DishBriefVO> selectDishBrief(@Param("goodThreshold") Integer goodThreshold);

    // ===== 数据大屏 =====

    /** 大屏核心指标（含销量、营收、评分） */
    Map<String, Object> selectDashboardOverview(@Param("goodThreshold") Integer goodThreshold);

    /** 近 N 天销量与营收趋势（基准为销量数据最新日） */
    List<Map<String, Object>> selectSalesTrend(@Param("days") Integer days);

    /** 品类销量分布 */
    List<Map<String, Object>> selectCategorySales();

    /** 档口销量排行 */
    List<Map<String, Object>> selectStallSalesRank(@Param("limit") Integer limit);

    /** 菜品销量排行 */
    List<Map<String, Object>> selectDishSalesRank(@Param("limit") Integer limit);

    /** 评分分布 */
    List<Map<String, Object>> selectRatingDist();

    /** 各档口口碑（评分 + 好评率） */
    List<Map<String, Object>> selectStallRating(@Param("goodThreshold") Integer goodThreshold);
}
