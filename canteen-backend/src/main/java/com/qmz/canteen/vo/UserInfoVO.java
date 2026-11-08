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
    /** 账户余额（展示型） */
    private java.math.BigDecimal balance;
    private Integer status;
    private Long reviewCount;
    private Long favoriteCount;
    private Long feedbackCount;
    private LocalDateTime createTime;

    // -------- 商家账号关联档口信息（商家管理页使用） --------
    private String stallName;
    private String stallWindowNo;
    private String stallArea;
    private Integer stallFloor;
    private String stallCategory;
    private String managerName;
    private String managerPhone;
    private java.math.BigDecimal hygieneScore;
    private Long dishCount;
    private Double avgRating;
    private Long salesCount;
}
