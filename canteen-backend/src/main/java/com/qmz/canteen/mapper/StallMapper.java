package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.vo.StallVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StallMapper extends BaseMapper<Stall> {

    List<StallVO> selectStallWithStats(@Param("keyword") String keyword,
                                       @Param("area") String area,
                                       @Param("floor") Integer floor,
                                       @Param("goodThreshold") Integer goodThreshold);

    /** 按主键查询（不受营业状态限制，供管理员编辑歇业档口） */
    StallVO selectStallById(@Param("id") Long id, @Param("goodThreshold") Integer goodThreshold);
}
