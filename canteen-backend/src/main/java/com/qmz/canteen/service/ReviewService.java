package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.dto.ReviewRequest;
import com.qmz.canteen.vo.ReviewReplyVO;
import com.qmz.canteen.vo.ReviewVO;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    PageResult<ReviewVO> pageReviews(ReviewQuery query, Long currentUserId, boolean adminScope);

    /** 商家端：分页查询本档口菜品的学生评价 */
    PageResult<ReviewVO> pageMerchantReviews(Long stallId, ReviewQuery query);

    /** 商家回复学生评价，并将该评价标记为已处理 */
    void replyReview(Long reviewId, Long stallId, String reply);

    Long submitReview(ReviewRequest request, Long userId);

    void deleteReview(Long id, Long currentUserId, boolean isAdmin);

    void updateReviewStatus(Long id, Integer status);

    /** 点赞/取消点赞，返回 { liked, likeCount } */
    Map<String, Object> toggleLike(Long reviewId, Long userId);

    /** 学生对评价的追问/回答 */
    Long replyReviewByStudent(Long reviewId, Long userId, String content);

    /** 查询某评价的学生回复列表 */
    List<ReviewReplyVO> listReplies(Long reviewId, Long userId);

    /** 是否可评价该菜品（需下过单） */
    boolean canReview(Long dishId, Long userId);
}
