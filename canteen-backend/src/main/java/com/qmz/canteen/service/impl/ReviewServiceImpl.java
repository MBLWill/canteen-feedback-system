package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.dto.ReviewRequest;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DishReview;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishReviewMapper;
import com.qmz.canteen.service.ReviewService;
import com.qmz.canteen.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final DishReviewMapper dishReviewMapper;
    private final DishMapper dishMapper;

    @Override
    public PageResult<ReviewVO> pageReviews(ReviewQuery query, Long currentUserId, boolean adminScope) {
        if (!adminScope) {
            query.setStatus(DishReview.STATUS_PUBLISHED);
        }
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);
        IPage<ReviewVO> result = dishReviewMapper.selectReviewPage(new Page<>(current, size), query, currentUserId);
        return PageResult.of(result);
    }

    @Override
    @Transactional
    public Long submitReview(ReviewRequest request, Long userId) {
        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish == null || Integer.valueOf(0).equals(dish.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该菜品不存在或已下架");
        }

        // 必须包含已逻辑删除的记录：dish_review 上 (dish_id, user_id) 是唯一键，
        // 逻辑删除后记录仍占位，若只查未删除的会走 insert 并撞唯一键。
        DishReview review = dishReviewMapper.selectOneIncludeDeleted(request.getDishId(), userId);

        if (review == null) {
            review = new DishReview();
            review.setDishId(request.getDishId());
            review.setUserId(userId);
            // rating 为 NOT NULL 且无默认值，必须在 insert 前赋值，否则严格模式下插入失败
            review.setRating(request.getRating());
            review.setContent(request.getContent());
            review.setAnonymous(request.getAnonymous() == null ? 0 : request.getAnonymous());
            review.setStatus(DishReview.STATUS_PUBLISHED);
            dishReviewMapper.insert(review);
            return review.getId();
        }

        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setAnonymous(request.getAnonymous() == null ? 0 : request.getAnonymous());
        review.setStatus(DishReview.STATUS_PUBLISHED);
        // 若该记录曾被逻辑删除，这里一并复活
        review.setDeleted(0);
        dishReviewMapper.restoreAndUpdate(review);
        return review.getId();
    }

    @Override
    @Transactional
    public void deleteReview(Long id, Long currentUserId, boolean isAdmin) {
        DishReview review = dishReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评价不存在");
        }
        if (!isAdmin && !review.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能删除自己的评价");
        }
        dishReviewMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateReviewStatus(Long id, Integer status) {
        DishReview review = dishReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评价不存在");
        }
        review.setStatus(status);
        dishReviewMapper.updateById(review);
    }

    @Override
    public PageResult<ReviewVO> pageMerchantReviews(Long stallId, ReviewQuery query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);
        IPage<ReviewVO> result = dishReviewMapper.selectMerchantReviewPage(new Page<>(current, size), query, stallId);
        return PageResult.of(result);
    }

    @Override
    @Transactional
    public void replyReview(Long reviewId, Long stallId, String reply) {
        String content = reply == null ? "" : reply.trim();
        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "回复内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "回复内容不能超过500字");
        }
        DishReview review = dishReviewMapper.selectByIdAndStallId(reviewId, stallId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评价不存在或不属于本档口");
        }
        review.setMerchantReply(content);
        review.setMerchantReplyTime(LocalDateTime.now());
        review.setHandled(1);
        dishReviewMapper.updateById(review);
    }
}
