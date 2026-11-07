package com.qmz.canteen.security;

import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器，支持两种模式：
 * <ul>
 *   <li>强制登录：无 token 或 token 无效直接抛 401</li>
 *   <li>可选登录（白名单路径）：有 token 就解析并填充 UserContext，没有也放行</li>
 * </ul>
 * 白名单路径同样要解析身份，否则 /dishes、/stalls 等公开接口里的
 * UserContext.getUserId() 永远为 null，会导致「已收藏」「我的评价」等
 * 依赖当前用户的字段全部失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        LoginUser user = resolveUser(request);
        boolean optional = OptionalAuthPaths.matches(request.getRequestURI(), request.getContextPath());

        if (user == null) {
            if (optional) {
                return true;
            }
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        UserContext.set(user);
        return true;
    }

    private LoginUser resolveUser(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(PREFIX)) {
            return null;
        }
        return jwtUtil.parseLoginUser(header.substring(PREFIX.length()));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
