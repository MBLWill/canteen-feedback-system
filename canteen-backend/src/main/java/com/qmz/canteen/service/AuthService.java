package com.qmz.canteen.service;

import com.qmz.canteen.dto.LoginRequest;
import com.qmz.canteen.dto.ProfileUpdateRequest;
import com.qmz.canteen.dto.RegisterRequest;
import com.qmz.canteen.vo.LoginVO;
import com.qmz.canteen.vo.UserInfoVO;

public interface AuthService {

    LoginVO login(LoginRequest request, String ip);

    void register(RegisterRequest request);

    UserInfoVO getProfile(Long userId);

    UserInfoVO updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword, String verifyCode);

    /** 发送改密码验证码（演示模式直接返回验证码） */
    String sendVerifyCode(Long userId, String target);
}
