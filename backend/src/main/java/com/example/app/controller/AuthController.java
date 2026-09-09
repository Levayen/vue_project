package com.example.app.controller;

import com.example.app.dto.ChangePasswordRequest;
import com.example.app.dto.LoginRequest;
import com.example.app.dto.LoginResponse;
import com.example.app.dto.UserDTO;
import com.example.app.security.UserContext;
import com.example.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口（SPEC-identity）
 * POST /api/auth/login 登录（拦截器放行）；GET /api/auth/me 当前用户（需登录）；
 * POST /api/auth/change-password 修改密码（需登录，Issue1）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public UserDTO me() {
        Long userId = UserContext.currentUserId();
        return authService.currentUser(userId);
    }

    /**
     * 修改当前登录用户密码（首次登录强制改密 / 自助改密）
     */
    @PostMapping("/change-password")
    public UserDTO changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.currentUserId();
        return authService.changePassword(userId, request.oldPassword(), request.newPassword());
    }
}
