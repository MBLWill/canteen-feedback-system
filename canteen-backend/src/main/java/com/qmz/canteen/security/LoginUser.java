package com.qmz.canteen.security;

import lombok.Data;

@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String realName;
    private String role;
    /** 商家所属档口ID，仅 MERCHANT 角色有值 */
    private Long stallId;

    public boolean isAdmin() {
        return UserRole.ADMIN.name().equals(this.role);
    }

    public boolean isMerchant() {
        return UserRole.MERCHANT.name().equals(this.role);
    }
}
