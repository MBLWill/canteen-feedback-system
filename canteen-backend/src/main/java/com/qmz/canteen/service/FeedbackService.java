package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.FeedbackQuery;
import com.qmz.canteen.dto.FeedbackRequest;
import com.qmz.canteen.dto.ReplyRequest;
import com.qmz.canteen.vo.FeedbackVO;

public interface FeedbackService {

    PageResult<FeedbackVO> pageFeedbacks(FeedbackQuery query, Long userId, String scope);

    Long createFeedback(FeedbackRequest request, Long userId);

    void replyFeedback(Long id, ReplyRequest request, Long adminId);

    void updateStatus(Long id, String status, Long currentUserId, boolean isAdmin);

    void deleteFeedback(Long id, Long currentUserId, boolean isAdmin);
}
