package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 30, message = "账号长度不合法")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 50, message = "密码长度不合法")
    private String password;
}
