package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmz.canteen.entity.ReviewReply;
import com.qmz.canteen.vo.ReviewReplyVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ReviewReplyMapper extends BaseMapper<ReviewReply> {

    /** 查询某评价的回复列表（含回复人用户名/头像） */
    @Select("SELECT rr.id, rr.review_id AS reviewId, rr.user_id AS userId, rr.content, rr.create_time AS createTime, "
            + "IFNULL(u.real_name, '已注销用户') AS userName, IFNULL(u.avatar, '') AS avatar, "
            + "(rr.user_id = #{currentUserId}) AS mine "
            + "FROM review_reply rr "
            + "LEFT JOIN sys_user u ON rr.user_id = u.id AND u.deleted = 0 "
            + "WHERE rr.review_id = #{reviewId} AND rr.deleted = 0 "
            + "ORDER BY rr.create_time ASC")
    List<ReviewReplyVO> selectReplies(@Param("reviewId") Long reviewId,
                                      @Param("currentUserId") Long currentUserId);
}
