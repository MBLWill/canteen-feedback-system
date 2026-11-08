package com.qmz.canteen.controller;

import com.qmz.canteen.common.IpUtils;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.LoginRequest;
import com.qmz.canteen.dto.PasswordChangeRequest;
import com.qmz.canteen.dto.ProfileUpdateRequest;
import com.qmz.canteen.dto.RegisterRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.AuthService;
import com.qmz.canteen.vo.AccountSummaryVO;
import com.qmz.canteen.vo.LoginVO;
import com.qmz.canteen.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, IpUtils.getClientIp(httpRequest)));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @GetMapping("/profile")
    public Result<UserInfoVO> profile() {
        return Result.success(authService.getProfile(UserContext.getUserId()));
    }

    /** 我的账户统计（余额 / 总订单 / 总消费 / 已完成） */
    @GetMapping("/account-summary")
    public Result<AccountSummaryVO> accountSummary() {
        return Result.success(authService.getAccountSummary(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        return Result.success(authService.updateProfile(UserContext.getUserId(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid PasswordChangeRequest request) {
        authService.changePassword(UserContext.getUserId(), request.getOldPassword(),
                request.getNewPassword(), request.getVerifyCode());
        return Result.success();
    }

    /** 发送改密码验证码到绑定的邮箱/电话（演示模式直接返回验证码） */
    @PostMapping("/verify-code")
    public Result<String> sendVerifyCode(@RequestBody java.util.Map<String, String> body) {
        return Result.success(authService.sendVerifyCode(UserContext.getUserId(), body.get("target")));
    }
}
