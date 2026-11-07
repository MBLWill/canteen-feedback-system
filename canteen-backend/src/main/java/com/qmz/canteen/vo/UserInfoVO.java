package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String roleLabel;
    /** 商家所属档口ID */
    private Long stallId;
    private String studentNo;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private Long reviewCount;
    private Long favoriteCount;
    private Long feedbackCount;
    private LocalDateTime createTime;
}
