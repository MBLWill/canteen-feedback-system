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
}
