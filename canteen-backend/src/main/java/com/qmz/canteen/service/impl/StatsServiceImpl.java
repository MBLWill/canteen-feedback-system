package com.qmz.canteen.service.impl;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.mapper.StatsMapper;
import com.qmz.canteen.service.StatsService;
import com.qmz.canteen.vo.DishRankVO;
import com.qmz.canteen.vo.DishScatterVO;
import com.qmz.canteen.vo.OverviewVO;
import com.qmz.canteen.vo.StallGoodRateVO;
import com.qmz.canteen.vo.StallPopularityVO;
import com.qmz.canteen.vo.StallScatterVO;
import com.qmz.canteen.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    /** 评价次数低于该值的菜品视为新菜品，样本不足不足以判定红黑榜 */
    private static final int NEW_DISH_REVIEW_THRESHOLD = 10;
    private static final double VALUE_RATING = 4.0;
    private static final double AVOID_RATING = 3.2;
    private static final double QUALITY_RATING = 3.8;
    private static final double QUALITY_HYGIENE = 85.0;
    private static final double BAD_RATING = 3.5;
    private static final double BAD_HYGIENE = 80.0;

    @Value("${canteen.good-rating-threshold}")
    private Integer goodThreshold;

    private final StatsMapper statsMapper;

    @Override
    public OverviewVO overview() {
        Map<String, Object> data = statsMapper.selectOverview(goodThreshold);
        OverviewVO vo = new OverviewVO();
        vo.setStallCount(toLong(data.get("stallCount")));
        vo.setDishCount(toLong(data.get("dishCount")));
        vo.setReviewCount(toLong(data.get("reviewCount")));
        vo.setFeedbackCount(toLong(data.get("feedbackCount")));
        vo.setPendingFeedbackCount(toLong(data.get("pendingFeedbackCount")));
        vo.setStudentCount(toLong(data.get("studentCount")));
        vo.setFavoriteCount(toLong(data.get("favoriteCount")));
        vo.setAvgRating(toDouble(data.get("avgRating")));
        vo.setGoodRate(toDouble(data.get("goodRate")));
        vo.setTodayReviewCount(toLong(data.get("todayReviewCount")));
        return vo;
    }

    @Override
    public List<DishRankVO> dishRanking(Long limit, String period) {
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        String p = period == null ? "all" : period;
        return statsMapper.selectDishRanking(size, goodThreshold, p);
    }

    @Override
    public List<StallGoodRateVO> stallGoodRate() {
        return statsMapper.selectStallGoodRate(goodThreshold);
    }

    @Override
    public List<StallPopularityVO> stallTop(Long limit) {
        long size = limit == null || limit < 1 ? 3 : Math.min(limit, 20);
        List<StallPopularityVO> list = statsMapper.selectStallPopularity(size);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }
        return list;
    }

    @Override
    public List<TrendVO> ratingTrend(Integer days) {
        int d = days == null || days < 1 ? 7 : Math.min(days, 90);
        return statsMapper.selectRatingTrend(d);
    }

    @Override
    public List<DishScatterVO> dishScatter(Long limit) {
        List<DishScatterVO> list = statsMapper.selectDishScatter(goodThreshold);
        if (list.isEmpty()) {
            return list;
        }
        // 高性价比与避雷都是相对全站均价而言的
        double avgPrice = list.stream()
                .mapToDouble(d -> d.getPrice() == null ? 0 : d.getPrice().doubleValue())
                .average().orElse(0);

        for (DishScatterVO d : list) {
            double rating = d.getAvgRating() == null ? 0 : d.getAvgRating();
            double price = d.getPrice() == null ? 0 : d.getPrice().doubleValue();
            long count = d.getReviewCount() == null ? 0 : d.getReviewCount();

            String tag;
            String label;
            // 样本量不足时先标新菜品，避免个别差评把刚上架的菜误判成避雷
            if (count < NEW_DISH_REVIEW_THRESHOLD) {
                tag = "new";
                label = "新菜品";
            } else if (rating <= AVOID_RATING && price >= avgPrice) {
                tag = "avoid";
                label = "避雷";
            } else if (rating >= VALUE_RATING && price <= avgPrice) {
                tag = "value";
                label = "高性价比";
            } else {
                tag = "normal";
                label = "一般";
            }
            d.setTag(tag);
            d.setTagLabel(label);
        }

        // 涟漪点优先展示，普通点靠后截断，避免图上全是灰点
        list.sort(Comparator.comparing((DishScatterVO d) -> tagWeight(d.getTag()))
                .thenComparing(Comparator.comparing(DishScatterVO::getReviewCount).reversed()));

        long size = limit == null || limit < 1 ? list.size() : Math.min(limit, list.size());
        return list.subList(0, (int) size);
    }

    @Override
    public List<StallScatterVO> stallScatter() {
        List<StallScatterVO> list = statsMapper.selectStallScatter(goodThreshold);
        for (StallScatterVO s : list) {
            double rating = s.getAvgRating() == null ? 0 : s.getAvgRating();
            double hygiene = s.getHygieneScore() == null ? 0 : s.getHygieneScore().doubleValue();

            String tag;
            String label;
            if (rating >= QUALITY_RATING && hygiene >= QUALITY_HYGIENE) {
                tag = "quality";
                label = "优质档口";
            } else if (rating < BAD_RATING && hygiene < BAD_HYGIENE) {
                tag = "bad";
                label = "劣质档口";
            } else if (rating >= QUALITY_RATING) {
                tag = "normal";
                label = "评分高但卫生待提升";
            } else if (hygiene >= QUALITY_HYGIENE) {
                tag = "normal";
                label = "卫生达标但口碑一般";
            } else {
                tag = "normal";
                label = "一般";
            }
            s.setTag(tag);
            s.setTagLabel(label);
            s.setComposite(round1(rating / 5 * 60 + hygiene / 100 * 40));
        }
        list.sort(Comparator.comparing(StallScatterVO::getComposite).reversed());
        return list;
    }

    private static int tagWeight(String tag) {
        return switch (tag == null ? "" : tag) {
            case "value", "avoid" -> 0;
            case "new" -> 1;
            default -> 2;
        };
    }

    private static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        return value == null ? 0D : ((Number) value).doubleValue();
    }
}
