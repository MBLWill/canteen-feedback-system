package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.vo.UserInfoVO;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    /** 商家账号分页：账号信息 + 所属档口经营数据 */
    IPage<UserInfoVO> selectMerchantPage(IPage<UserInfoVO> page,
                                         @Param("keyword") String keyword,
                                         @Param("status") Integer status);
}
