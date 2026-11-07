package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qmz.canteen.dto.FeedbackQuery;
import com.qmz.canteen.entity.Feedback;
import com.qmz.canteen.vo.FeedbackVO;
import org.apache.ibatis.annotations.Param;

public interface FeedbackMapper extends BaseMapper<Feedback> {

    IPage<FeedbackVO> selectFeedbackPage(IPage<?> page, @Param("query") FeedbackQuery query,
                                         @Param("userId") Long userId, @Param("scope") String scope);
}
