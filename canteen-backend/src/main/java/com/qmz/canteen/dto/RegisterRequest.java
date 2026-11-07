package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "账号为4-20位字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过20字")
    private String realName;

    @Size(max = 30, message = "学号不能超过30位")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "学号只能为字母或数字")
    private String studentNo;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
