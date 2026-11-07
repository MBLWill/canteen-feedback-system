package com.qmz.canteen.security;

import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;

public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && user.isAdmin();
    }

    public static boolean isMerchant() {
        LoginUser user = HOLDER.get();
        return user != null && user.isMerchant();
    }

    /** 商家所属档口ID；非商家返回 null */
    public static Long merchantStallId() {
        LoginUser user = HOLDER.get();
        return user != null && user.isMerchant() ? user.getStallId() : null;
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    /** 要求商家身份并返回其档口ID */
    public static Long requireMerchant() {
        Long stallId = merchantStallId();
        if (stallId == null) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return stallId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
