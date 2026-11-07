package com.qmz.canteen.controller;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.FeedbackQuery;
import com.qmz.canteen.dto.FeedbackRequest;
import com.qmz.canteen.dto.ReplyRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.FeedbackService;
import com.qmz.canteen.vo.FeedbackVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public Result<PageResult<FeedbackVO>> page(@Valid FeedbackQuery query) {
        // 管理员默认看全部；若前端显式传 scope=MINE（如管理员在「日常使用」里看自己的留言），则尊重该值
        String scope = "MINE".equals(query.getScope())
                ? "MINE"
                : (UserContext.isAdmin() ? "ALL" : "MINE");
        return Result.success(feedbackService.pageFeedbacks(query, UserContext.getUserId(), scope));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid FeedbackRequest request) {
        return Result.success(feedbackService.createFeedback(request, UserContext.getUserId()));
    }

    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable @Positive(message = "留言ID不合法") Long id,
                              @RequestBody @Valid ReplyRequest request) {
        UserContext.requireAdmin();
        feedbackService.replyFeedback(id, request, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable @Positive(message = "留言ID不合法") Long id,
                                     @RequestParam @Size(max = 20, message = "状态取值不合法")
                                     @Pattern(regexp = "^(PENDING|PROCESSING|RESOLVED)$",
                                             message = "状态取值不合法") String status) {
        feedbackService.updateStatus(id, status, UserContext.getUserId(), UserContext.isAdmin());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive(message = "留言ID不合法") Long id) {
        feedbackService.deleteFeedback(id, UserContext.getUserId(), UserContext.isAdmin());
        return Result.success();
    }
}
