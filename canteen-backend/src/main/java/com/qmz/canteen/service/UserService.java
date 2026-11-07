package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.UserQuery;
import com.qmz.canteen.vo.UserInfoVO;

public interface UserService {

    PageResult<UserInfoVO> pageUsers(UserQuery query);

    /** 商家账号分页，附带所属档口经营信息 */
    PageResult<UserInfoVO> pageMerchants(UserQuery query);

    void updateStatus(Long id, Integer status, Long currentUserId);
}
