package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmz.canteen.entity.ChatLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ChatLogMapper extends BaseMapper<ChatLog> {

    /** 未命中问题排行，用于管理员补录知识库 */
    @Select("SELECT question AS question, COUNT(*) AS cnt, MAX(create_time) AS lastTime " +
            "FROM chat_log WHERE hit = 0 " +
            "GROUP BY question ORDER BY cnt DESC, lastTime DESC LIMIT #{limit}")
    List<Map<String, Object>> selectMissTop(@Param("limit") Long limit);
}
