package com.example.app.dto;

import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;

/**
 * 用户信息视图对象（SPEC-identity）
 * 不含密码哈希等敏感字段。
 */
public record UserDTO(
        Long id,
        String username,
        UserRole role,
        Long studentId,
        Boolean enabled
) {
    public static UserDTO from(SysUser user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStudentId(),
                user.getEnabled()
        );
    }
}
