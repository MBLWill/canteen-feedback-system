package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmz.canteen.entity.Notification;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface NotificationMapper extends BaseMapper<Notification> {

    /** 某用户的未读通知数 */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);

    /** 某用户的全部通知（按时间倒序） */
    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY create_time DESC, id DESC LIMIT #{limit}")
    List<Notification> listByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /** 全部标记已读 */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
