package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.dto.ReviewRequest;
import com.qmz.canteen.vo.ReviewVO;

public interface ReviewService {

    PageResult<ReviewVO> pageReviews(ReviewQuery query, Long currentUserId, boolean adminScope);

    Long submitReview(ReviewRequest request, Long userId);

    void deleteReview(Long id, Long currentUserId, boolean isAdmin);

    void updateReviewStatus(Long id, Integer status);
}
