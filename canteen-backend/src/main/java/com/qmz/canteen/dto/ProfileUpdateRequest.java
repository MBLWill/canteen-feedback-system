package com.qmz.canteen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(min = 1, max = 50, message = "姓名不能超过50字")
    private String realName;

    @Size(max = 100, message = "邮箱不能超过100字")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^(1[3-9]\\d{9}|0\\d{2,3}-?\\d{7,8})?$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 255, message = "头像地址不合法")
    private String avatar;
}
