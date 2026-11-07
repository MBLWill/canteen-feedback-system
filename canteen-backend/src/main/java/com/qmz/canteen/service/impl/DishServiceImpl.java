package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.DishQuery;
import com.qmz.canteen.dto.DishRequest;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.entity.CartItem;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DishReview;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.mapper.CartItemMapper;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishReviewMapper;
import com.qmz.canteen.mapper.StallMapper;
import com.qmz.canteen.mapper.StatsMapper;
import com.qmz.canteen.service.DishService;
import com.qmz.canteen.vo.DishDetailVO;
import com.qmz.canteen.vo.DishVO;
import com.qmz.canteen.vo.RatingDistributionVO;
import com.qmz.canteen.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    @Value("${canteen.good-rating-threshold}")
    private Integer goodThreshold;

    private final DishMapper dishMapper;
    private final StallMapper stallMapper;
    private final DishReviewMapper dishReviewMapper;
    private final StatsMapper statsMapper;
    private final CartItemMapper cartItemMapper;

    @Override
    public PageResult<DishVO> pageDishes(DishQuery query, Long userId) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 12 : Math.min(query.getSize(), 100);
        Page<DishVO> page = new Page<>(current, size);
        IPage<DishVO> result = dishMapper.selectDishPage(page, query, userId, goodThreshold, resolveOrder(query.getSortBy()));
        return PageResult.of(result);
    }

    @Override
    public DishDetailVO getDishDetail(Long dishId, Long userId) {
        DishVO dish = dishMapper.selectDishDetail(dishId, userId, goodThreshold);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "菜品不存在或已下架");
        }

        ReviewQuery reviewQuery = new ReviewQuery();
        reviewQuery.setDishId(dishId);
        reviewQuery.setStatus(DishReview.STATUS_PUBLISHED);
        IPage<ReviewVO> latest = dishReviewMapper.selectReviewPage(new Page<>(1, 5), reviewQuery, userId);

        DishDetailVO detail = new DishDetailVO();
        detail.setDish(dish);
        detail.setDistribution(buildDistribution(dishId));
        detail.setLatestReviews(latest.getRecords());
        detail.setRecommendDishes(dishMapper.selectRecommendDishes(dishId, 4L, goodThreshold));
        return detail;
    }

    @Override
    public Long createDish(DishRequest request, Long operatorStallId) {
        // 商家只能给自己档口加菜，强制覆盖请求里的 stallId，防越权
        if (operatorStallId != null) {
            request.setStallId(operatorStallId);
        }
        Stall stall = stallMapper.selectById(request.getStallId());
        if (stall == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "所属档口不存在");
        }
        validatePrice(request.getPrice());
        String name = normalize(request.getName());
        ensureNameAvailable(request.getStallId(), name, null);

        Dish dish = new Dish();
        BeanUtils.copyProperties(request, dish);
        dish.setName(name);
        dish.setCategory(normalize(request.getCategory()));
        dish.setStatus(dish.getStatus() == null ? 1 : dish.getStatus());
        try {
            dishMapper.insert(dish);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过上面的查重，最终由唯一键兜底
            throw duplicateName(name);
        }
        return dish.getId();
    }

    @Override
    public void updateDish(Long id, DishRequest request, Long operatorStallId) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "菜品不存在");
        }
        if (operatorStallId != null && !operatorStallId.equals(dish.getStallId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能管理自己档口的菜品");
        }
        // 允许改到别的档口，但新档口必须存在，否则会留下悬空 stall_id
        if (!request.getStallId().equals(dish.getStallId())
                && stallMapper.selectById(request.getStallId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "所属档口不存在");
        }
        validatePrice(request.getPrice());
        String name = normalize(request.getName());
        ensureNameAvailable(request.getStallId(), name, id);

        BeanUtils.copyProperties(request, dish);
        dish.setName(name);
        dish.setCategory(normalize(request.getCategory()));
        try {
            dishMapper.updateById(dish);
        } catch (DuplicateKeyException e) {
            throw duplicateName(name);
        }
    }

    /** 同档口菜品重名检查；deleted=1 的历史行不参与（与唯一键语义保持一致） */
    private void ensureNameAvailable(Long stallId, String name, Long excludeId) {
        long count = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStallId, stallId)
                .eq(Dish::getName, name)
                .ne(excludeId != null, Dish::getId, excludeId));
        if (count > 0) {
            throw duplicateName(name);
        }
    }

    private BusinessException duplicateName(String name) {
        return new BusinessException(ResultCode.DATA_EXISTS.getCode(),
                "该档口下已存在同名菜品「" + name + "」");
    }

    private String normalize(String text) {
        return StringUtils.hasText(text) ? text.trim() : text;
    }

    @Override
    public void deleteDish(Long id, Long operatorStallId) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "菜品不存在");
        }
        if (operatorStallId != null && !operatorStallId.equals(dish.getStallId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能管理自己档口的菜品");
        }
        dishMapper.deleteById(id);
        // 菜下架了但购物车里还留着，结算时价格会变成空值，这里一并清掉
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getDishId, id));
    }

    @Override
    public List<String> listCategories() {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .select(Dish::getCategory)
                .eq(Dish::getStatus, 1)
                .isNotNull(Dish::getCategory)
                .ne(Dish::getCategory, ""));
        return dishes.stream().map(Dish::getCategory).distinct().sorted().toList();
    }

    private void validatePrice(BigDecimal price) {
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "价格不能为负数");
        }
    }

    /** 排序字段白名单，避免 SQL 注入 */
    private String resolveOrder(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "avg_rating DESC, review_count DESC";
        }
        return switch (sortBy) {
            case "reviews" -> "review_count DESC, avg_rating DESC";
            // 销量优先：销售记录 + 已支付订单的累计份数
            case "sales" -> "sales_count DESC, avg_rating DESC, review_count DESC";
            // 综合：按档口窗口号，南区 S101→S102→S201→S202，再到北区 N101→N102
            case "comprehensive" -> "CASE s.area WHEN 'south' THEN 0 ELSE 1 END ASC, s.window_no ASC, d.id ASC";
            case "priceAsc" -> "d.price ASC";
            case "priceDesc" -> "d.price DESC";
            case "newest" -> "d.create_time DESC";
            default -> "avg_rating DESC, review_count DESC";
        };
    }

    private RatingDistributionVO buildDistribution(Long dishId) {
        Map<String, Object> data = statsMapper.selectRatingDistribution(dishId);
        RatingDistributionVO vo = new RatingDistributionVO();
        vo.setStar5(toLong(data.get("star5")));
        vo.setStar4(toLong(data.get("star4")));
        vo.setStar3(toLong(data.get("star3")));
        vo.setStar2(toLong(data.get("star2")));
        vo.setStar1(toLong(data.get("star1")));
        vo.setTotal(toLong(data.get("total")));
        return vo;
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
