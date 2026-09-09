package com.example.app.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建/更新账号请求（SPEC-identity，管理员接口）
 * 创建时 username/password/role 必填；更新时仅 role/studentId/enabled 生效，密码走重置接口。
 */
public record SaveUserRequest(
        @NotBlank(message = "用户名不能为空")
        String username,
        String password,
        @NotBlank(message = "角色不能为空")
        String role,
        Long studentId,
        Boolean enabled
) {}
