package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.common.enums.FeedbackStatus;
import com.qmz.canteen.common.enums.FeedbackType;
import com.qmz.canteen.dto.FeedbackQuery;
import com.qmz.canteen.dto.FeedbackRequest;
import com.qmz.canteen.dto.ReplyRequest;
import com.qmz.canteen.entity.Feedback;
import com.qmz.canteen.mapper.FeedbackMapper;
import com.qmz.canteen.service.FeedbackService;
import com.qmz.canteen.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;

    @Override
    public PageResult<FeedbackVO> pageFeedbacks(FeedbackQuery query, Long userId, String scope) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);
        IPage<FeedbackVO> result = feedbackMapper.selectFeedbackPage(new Page<>(current, size), query, userId, scope);
        return PageResult.of(result);
    }

    @Override
    @Transactional
    public Long createFeedback(FeedbackRequest request, Long userId) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setStallId(request.getStallId());
        feedback.setTitle(request.getTitle());
        feedback.setContent(request.getContent());
        feedback.setType(FeedbackType.of(request.getType()).name());
        feedback.setStatus(FeedbackStatus.PENDING.name());
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    @Override
    @Transactional
    public void replyFeedback(Long id, ReplyRequest request, Long adminId) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "留言不存在");
        }
        feedback.setReplyContent(request.getReplyContent());
        feedback.setReplyUserId(adminId);
        feedback.setReplyTime(LocalDateTime.now());
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus() : FeedbackStatus.PROCESSING.name();
        feedback.setStatus(FeedbackStatus.of(status).name());
        feedbackMapper.updateById(feedback);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status, Long currentUserId, boolean isAdmin) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "留言不存在");
        }
        if (!isAdmin && !feedback.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "没有权限操作该留言");
        }
        feedback.setStatus(FeedbackStatus.of(status).name());
        feedbackMapper.updateById(feedback);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id, Long currentUserId, boolean isAdmin) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "留言不存在");
        }
        boolean owner = feedback.getUserId().equals(currentUserId);
        if (!isAdmin && !owner) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "没有权限删除该留言");
        }
        if (!isAdmin && !FeedbackStatus.PENDING.name().equals(feedback.getStatus())) {
            throw new BusinessException("留言已受理，无法删除");
        }
        feedbackMapper.deleteById(id);
    }
}
