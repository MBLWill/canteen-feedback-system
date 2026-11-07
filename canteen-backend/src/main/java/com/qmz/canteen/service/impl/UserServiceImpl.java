package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.UserQuery;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.UserMapper;
import com.qmz.canteen.security.UserRole;
import com.qmz.canteen.service.UserService;
import com.qmz.canteen.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public PageResult<UserInfoVO> pageUsers(UserQuery query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 商家账号统一在「商家管理」中维护，这里不再重复展示
        wrapper.ne(User::getRole, UserRole.MERCHANT.name());
        wrapper.and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(User::getUsername, query.getKeyword())
                        .or().like(User::getRealName, query.getKeyword())
                        .or().like(User::getStudentNo, query.getKeyword()))
                .eq(StringUtils.hasText(query.getRole()), User::getRole, query.getRole())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .orderByDesc(User::getCreateTime);

        IPage<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page, this::toVO);
    }

    @Override
    public PageResult<UserInfoVO> pageMerchants(UserQuery query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);

        IPage<UserInfoVO> page = userMapper.selectMerchantPage(
                new Page<>(current, size),
                StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null,
                query.getStatus());
        page.getRecords().forEach(vo -> vo.setRoleLabel(UserRole.MERCHANT.getLabel()));
        return PageResult.of(page);
    }

    @Override
    public void updateStatus(Long id, Integer status, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("不能修改自己的账号状态");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private UserInfoVO toVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoleLabel(UserRole.valueOf(user.getRole()).getLabel());
        return vo;
    }
}
