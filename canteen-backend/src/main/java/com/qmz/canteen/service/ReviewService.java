package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.dto.ReviewRequest;
import com.qmz.canteen.vo.ReviewVO;

public interface ReviewService {

    PageResult<ReviewVO> pageReviews(ReviewQuery query, Long currentUserId, boolean adminScope);

    /** 商家端：分页查询本档口菜品的学生评价 */
    PageResult<ReviewVO> pageMerchantReviews(Long stallId, ReviewQuery query);

    /** 商家回复学生评价，并将该评价标记为已处理 */
    void replyReview(Long reviewId, Long stallId, String reply);

    Long submitReview(ReviewRequest request, Long userId);

    void deleteReview(Long id, Long currentUserId, boolean isAdmin);

    void updateReviewStatus(Long id, Integer status);
}
