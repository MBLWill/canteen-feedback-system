package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmz.canteen.entity.ReviewLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReviewLikeMapper extends BaseMapper<ReviewLike> {

    /** 查询某用户是否已点赞某条评价 */
    @Select("SELECT COUNT(*) FROM review_like WHERE review_id = #{reviewId} AND user_id = #{userId}")
    int countByReviewAndUser(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
}
