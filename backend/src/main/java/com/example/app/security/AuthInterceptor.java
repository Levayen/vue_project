package com.example.app.security;

import com.example.app.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 认证与授权拦截器（SPEC-identity）
 * 1. 校验 Authorization: Bearer <token>，合法则写入 UserContext；
 * 2. 按路径前缀做角色控制：/api/admin/** 仅 ADMIN，/api/teacher/** 教师或管理员，/api/student/** 仅学生。
 * 放行路径在 WebConfig 中配置（登录接口）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<UserRole> TEACHER_OR_ADMIN = Set.of(UserRole.TEACHER, UserRole.ADMIN);

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return writeError(response, 401, "未登录或缺少认证令牌");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        final Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return writeError(response, 401, "认证令牌无效或已过期");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(claims.get("role", String.class));
        } catch (IllegalArgumentException | NullPointerException e) {
            return writeError(response, 401, "认证令牌角色信息异常");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        UserContext.set(new UserContext.CurrentUser(userId, username, role));

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin/") && role != UserRole.ADMIN) {
            return writeError(response, 403, "无权限：需要管理员角色");
        }
        if (uri.startsWith("/api/teacher/") && !TEACHER_OR_ADMIN.contains(role)) {
            return writeError(response, 403, "无权限：需要教师角色");
        }
        if (uri.startsWith("/api/student/") && role != UserRole.STUDENT) {
            return writeError(response, 403, "无权限：需要学生角色");
        }

        // 方法/类级 @RequireRoles 注解校验（多角色共用接口）
        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRoles requireRoles = handlerMethod.getMethodAnnotation(RequireRoles.class);
            if (requireRoles == null) {
                requireRoles = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
            }
            if (requireRoles != null && !Set.of(requireRoles.value()).contains(role)) {
                return writeError(response, 403, "无权限：当前角色不可访问该资源");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
        return false;
    }
}
