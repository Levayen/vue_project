package com.example.app.security;

import com.example.app.entity.UserRole;

/**
 * 当前登录用户上下文（SPEC-identity）
 * 基于 ThreadLocal，在 AuthInterceptor 中写入、请求结束后清理。
 */
public final class UserContext {

    private UserContext() {}

    /**
     * 当前登录用户信息
     */
    public record CurrentUser(Long userId, String username, UserRole role) {}

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户ID，未登录返回 null
     */
    public static Long currentUserId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    /**
     * 清理线程变量，防止线程池复用导致用户串号
     */
    public static void clear() {
        HOLDER.remove();
    }
}
