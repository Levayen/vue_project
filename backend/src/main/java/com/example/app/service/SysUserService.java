package com.example.app.service;

import com.example.app.dto.SaveUserRequest;
import com.example.app.dto.UserDTO;
import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;
import com.example.app.exception.BusinessException;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统账号管理服务（SPEC-identity，管理员功能）
 * 账号 CRUD、重置密码、启停；学生账号必须关联有效学生ID，教师/管理员不允许关联学生。
 */
@Service
public class SysUserService {

    private final SysUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public SysUserService(SysUserRepository userRepository,
                          StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 账号列表
     */
    public List<UserDTO> list() {
        return userRepository.findAll().stream().map(UserDTO::from).toList();
    }

    /**
     * 创建账号
     */
    @Transactional
    public UserDTO create(SaveUserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw BusinessException.badRequest("密码不能为空");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.conflict("用户名已存在");
        }
        UserRole role = parseRole(request.role());

        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setEnabled(request.enabled() == null || request.enabled());
        applyStudentId(user, role, request.studentId());

        return UserDTO.from(userRepository.save(user));
    }

    /**
     * 更新账号角色/学生关联/启停状态（用户名不可改，密码走重置接口）
     */
    @Transactional
    public UserDTO update(Long id, SaveUserRequest request) {
        SysUser user = requireUser(id);
        UserRole role = parseRole(request.role());
        user.setRole(role);
        applyStudentId(user, role, request.studentId());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return UserDTO.from(userRepository.save(user));
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw BusinessException.badRequest("新密码不能为空");
        }
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 删除账号；不允许删除当前登录账号
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw BusinessException.badRequest("不能删除当前登录账号");
        }
        SysUser user = requireUser(id);
        userRepository.delete(user);
    }

    private SysUser requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("账号不存在"));
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("非法角色：" + role);
        }
    }

    /**
     * 校验并设置学生关联：学生角色必须关联存在且未被占用的学生ID；其他角色不允许关联
     */
    private void applyStudentId(SysUser user, UserRole role, Long studentId) {
        if (role == UserRole.STUDENT) {
            if (studentId == null) {
                throw BusinessException.badRequest("学生账号必须关联学生");
            }
            if (!studentRepository.existsById(studentId)) {
                throw BusinessException.badRequest("关联学生不存在：" + studentId);
            }
            userRepository.findByStudentId(studentId).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw BusinessException.conflict("该学生已绑定其他账号");
                }
            });
            user.setStudentId(studentId);
        } else {
            if (studentId != null) {
                throw BusinessException.badRequest("教师/管理员账号不能关联学生");
            }
            user.setStudentId(null);
        }
    }
}
