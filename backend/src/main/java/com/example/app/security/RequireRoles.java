package com.example.app.security;

import com.example.app.entity.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法/类级角色要求注解（SPEC-identity 授权扩展）
 * 用于路径前缀无法表达的"多角色共用接口"（如题库 TEACHER+ADMIN）。
 * 由 AuthInterceptor 读取 HandlerMethod 上的注解进行校验。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRoles {
    UserRole[] value();
}
