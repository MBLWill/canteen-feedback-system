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
import com.qmz.canteen.entity.Notification;
import com.qmz.canteen.entity.ReviewLike;
import com.qmz.canteen.entity.ReviewReply;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishReviewMapper;
import com.qmz.canteen.mapper.NotificationMapper;
import com.qmz.canteen.mapper.ReviewLikeMapper;
import com.qmz.canteen.mapper.ReviewReplyMapper;
import com.qmz.canteen.service.ReviewService;
import com.qmz.canteen.vo.ReviewReplyVO;
import com.qmz.canteen.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final DishReviewMapper dishReviewMapper;
    private final DishMapper dishMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final NotificationMapper notificationMapper;

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

        // 下单后才能评价：存在该用户的非取消订单项才允许评价
        if (dishReviewMapper.countPurchased(request.getDishId(), userId) == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "购买过该菜品后才能评价哦");
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

        // 通知评价作者（学生）商家已回复
        if (review.getUserId() != null) {
            notifyUser(review.getUserId(), Notification.TYPE_MERCHANT_REPLY, reviewId, review.getDishId(), null, "商家回复了你的评价");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long reviewId, Long userId) {
        DishReview review = dishReviewMapper.selectById(reviewId);
        if (review == null || review.getDeleted() == null || review.getDeleted() != 0
                || Integer.valueOf(DishReview.STATUS_BLOCKED).equals(review.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评价不存在");
        }
        boolean liked = reviewLikeMapper.countByReviewAndUser(reviewId, userId) > 0;
        if (liked) {
            reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>()
                    .eq(ReviewLike::getReviewId, reviewId)
                    .eq(ReviewLike::getUserId, userId));
            dishReviewMapper.decrLike(reviewId);
        } else {
            ReviewLike like = new ReviewLike();
            like.setReviewId(reviewId);
            like.setUserId(userId);
            try {
                reviewLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发重复点赞：忽略，保持幂等
            }
            dishReviewMapper.incrLike(reviewId);
            if (review.getUserId() != null && !review.getUserId().equals(userId)) {
                notifyUser(review.getUserId(), Notification.TYPE_LIKE, reviewId, review.getDishId(), null, "有人点赞了你的评价");
            }
        }
        DishReview after = dishReviewMapper.selectById(reviewId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", !liked);
        result.put("likeCount", after.getLikeCount() == null ? 0 : after.getLikeCount());
        return result;
    }

    @Override
    @Transactional
    public Long replyReviewByStudent(Long reviewId, Long userId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "回复内容不能为空");
        }
        if (text.length() > 500) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "回复内容不能超过500字");
        }
        DishReview review = dishReviewMapper.selectById(reviewId);
        if (review == null || review.getDeleted() == null || review.getDeleted() != 0
                || Integer.valueOf(DishReview.STATUS_BLOCKED).equals(review.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评价不存在");
        }
        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setUserId(userId);
        reply.setContent(text);
        reviewReplyMapper.insert(reply);
        dishReviewMapper.incrReply(reviewId);

        if (review.getUserId() != null && !review.getUserId().equals(userId)) {
            notifyUser(review.getUserId(), Notification.TYPE_REPLY, reviewId, review.getDishId(), reply.getId(), "有人回复了你的评价");
        }
        return reply.getId();
    }

    @Override
    public List<ReviewReplyVO> listReplies(Long reviewId, Long userId) {
        return reviewReplyMapper.selectReplies(reviewId, userId);
    }

    @Override
    public boolean canReview(Long dishId, Long userId) {
        if (dishId == null || userId == null) {
            return false;
        }
        return dishReviewMapper.countPurchased(dishId, userId) > 0;
    }

    /** 写一条通知（内容统一前缀拼接，避免重复代码） */
    private void notifyUser(Long userId, String type, Long reviewId, Long dishId, Long replyId, String text) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setReviewId(reviewId);
        n.setDishId(dishId);
        n.setReplyId(replyId);
        n.setContent(text);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }
}
