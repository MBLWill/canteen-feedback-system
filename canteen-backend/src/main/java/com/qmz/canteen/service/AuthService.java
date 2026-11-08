package com.qmz.canteen.service;

import com.qmz.canteen.dto.LoginRequest;
import com.qmz.canteen.dto.ProfileUpdateRequest;
import com.qmz.canteen.dto.RegisterRequest;
import com.qmz.canteen.vo.AccountSummaryVO;
import com.qmz.canteen.vo.LoginVO;
import com.qmz.canteen.vo.UserInfoVO;

public interface AuthService {

    LoginVO login(LoginRequest request, String ip);

    void register(RegisterRequest request);

    UserInfoVO getProfile(Long userId);

    /** 我的账户统计（余额 / 总订单 / 总消费 / 已完成） */
    AccountSummaryVO getAccountSummary(Long userId);

    UserInfoVO updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword, String verifyCode);

    /** 发送改密码验证码（演示模式直接返回验证码） */
    String sendVerifyCode(Long userId, String target);
}
