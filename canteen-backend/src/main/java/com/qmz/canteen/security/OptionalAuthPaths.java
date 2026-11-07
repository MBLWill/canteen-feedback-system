package com.qmz.canteen.security;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 可选登录（匿名可访问）的路径白名单。
 * 与 WebConfig 的拦截器注册共用，避免两处规则不一致。
 */
public final class OptionalAuthPaths {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * 这些路径不强制登录：带 token 时解析身份（用于「是否已收藏」等个性化字段），
     * 不带 token 也放行。注意这里只放行 GET 浏览类接口，写操作在控制器内另有 requireAdmin 校验。
     */
    private static final List<String> PATTERNS = List.of(
            "/auth/login",
            "/auth/register",
            "/dishes",
            "/dishes/**",
            "/stalls",
            "/stalls/**",
            "/notices",
            "/notices/**",
            "/stats/overview",
            "/stats/dish-ranking",
            "/stats/stall-good-rate",
            "/stats/stall-top",
            "/stats/rating-trend",
            "/banners/list",
            "/pay/notify",
            "/pay/notify/**",
            "/uploads/**"
    );

    private OptionalAuthPaths() {
    }

    /**
     * @param uri         请求路径（不含 context-path）
     * @param contextPath 应用 context-path，例如 /api
     */
    public static boolean matches(String uri, String contextPath) {
        String path = uri;
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }
        for (String pattern : PATTERNS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
