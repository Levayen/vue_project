package com.example.app.service;

import com.example.app.dto.LoginResponse;
import com.example.app.dto.UserDTO;
import com.example.app.entity.SysUser;
import com.example.app.exception.BusinessException;
import com.example.app.repository.SysUserRepository;
import com.example.app.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务（SPEC-identity）
 * 登录校验、token 签发、当前用户查询。
 * 安全约定：用户不存在与密码错误返回相同提示；密码与 token 不打日志。
 */
@Service
public class AuthService {

    private final SysUserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SysUserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户名密码登录
     * @param username 用户名
     * @param rawPassword 明文密码
     * @return token 与用户信息
     */
    public LoginResponse login(String username, String rawPassword) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.unauthorized("用户名或密码错误"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw BusinessException.unauthorized("账号已停用，请联系管理员");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, UserDTO.from(user));
    }

    /**
     * 按ID查询当前登录用户，不存在抛 401
     */
    public UserDTO currentUser(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在或已失效"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw BusinessException.unauthorized("账号已停用，请联系管理员");
        }
        return UserDTO.from(user);
    }

    /**
     * 修改当前用户密码（Issue1：种子账号首次登录强制改密 / 用户自助改密）。
     * 校验原密码；新密码不得与原密码相同；成功后清除 mustChangePassword 标记。
     * 密码不打日志。
     */
    public UserDTO changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在或已失效"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw BusinessException.forbidden("账号已停用，请联系管理员");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (newPassword.equals(oldPassword)) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return UserDTO.from(user);
    }
}
