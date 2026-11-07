package com.qmz.canteen.controller;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.MerchantReviewReplyRequest;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.dto.ReviewRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.ReviewService;
import com.qmz.canteen.vo.ReviewVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public Result<PageResult<ReviewVO>> page(@Valid ReviewQuery query) {
        boolean admin = UserContext.isAdmin();
        return Result.success(reviewService.pageReviews(query, UserContext.getUserId(), admin));
    }

    @GetMapping("/my")
    public Result<PageResult<ReviewVO>> my(@Valid ReviewQuery query) {
        Long userId = UserContext.getUserId();
        query.setUserId(userId);
        return Result.success(reviewService.pageReviews(query, userId, UserContext.isAdmin()));
    }

    /** 商家端：本档口学生评价列表（支持 rating / handled 筛选与分页） */
    @GetMapping("/merchant")
    public Result<PageResult<ReviewVO>> merchantPage(@Valid ReviewQuery query) {
        Long stallId = UserContext.requireMerchant();
        return Result.success(reviewService.pageMerchantReviews(stallId, query));
    }

    /** 商家端：回复学生评价并标记为已处理 */
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable @Positive(message = "评价ID不合法") Long id,
                              @RequestBody @Valid MerchantReviewReplyRequest request) {
        Long stallId = UserContext.requireMerchant();
        reviewService.replyReview(id, stallId, request.getReply());
        return Result.success();
    }

    @PostMapping
    public Result<Long> submit(@RequestBody @Valid ReviewRequest request) {
        return Result.success(reviewService.submitReview(request, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive(message = "评价ID不合法") Long id) {
        reviewService.deleteReview(id, UserContext.getUserId(), UserContext.isAdmin());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable @Positive(message = "评价ID不合法") Long id,
                                     @RequestParam @Min(value = 0, message = "状态取值不合法")
                                     @Max(value = 2, message = "状态取值不合法") Integer status) {
        UserContext.requireAdmin();
        reviewService.updateReviewStatus(id, status);
        return Result.success();
    }
}
