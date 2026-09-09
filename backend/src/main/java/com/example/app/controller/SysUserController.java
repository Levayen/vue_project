package com.example.app.controller;

import com.example.app.dto.SaveUserRequest;
import com.example.app.dto.UserDTO;
import com.example.app.security.UserContext;
import com.example.app.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统账号管理接口（SPEC-identity）
 * 路径前缀 /api/admin/**，由 AuthInterceptor 限制仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/admin/users")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 账号列表
     */
    @GetMapping
    public List<UserDTO> list() {
        return sysUserService.list();
    }

    /**
     * 创建账号
     */
    @PostMapping
    public UserDTO create(@Valid @RequestBody SaveUserRequest request) {
        return sysUserService.create(request);
    }

    /**
     * 更新账号（角色/学生关联/启停）
     */
    @PutMapping("/{id}")
    public UserDTO update(@PathVariable Long id, @Valid @RequestBody SaveUserRequest request) {
        return sysUserService.update(id, request);
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        sysUserService.resetPassword(id, body.get("password"));
        return Map.of("message", "密码已重置");
    }

    /**
     * 删除账号（不能删除自己）
     */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        sysUserService.delete(id, UserContext.currentUserId());
        return Map.of("message", "账号已删除");
    }
}
