package com.example.app.dto;

/**
 * 登录成功响应（SPEC-identity）
 * @param token JWT 访问令牌
 * @param user 当前用户信息
 */
public record LoginResponse(String token, UserDTO user) {}
