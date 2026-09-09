package com.example.app.config;

import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.SysUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 种子数据初始化（SPEC-identity，M0 最小集；T26 会扩展课程/题库示例）
 * - 管理员 admin/admin123
 * - 教师 teacher/teacher123
 * - 为已存在学生按学号开通账号，密码 student123
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final SysUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserRepository userRepository,
                           StudentRepository studentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedTeacher();
        seedStudentAccounts();
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }

    private void seedTeacher() {
        if (userRepository.existsByUsername("teacher")) {
            return;
        }
        SysUser teacher = new SysUser();
        teacher.setUsername("teacher");
        teacher.setPasswordHash(passwordEncoder.encode("teacher123"));
        teacher.setRole(UserRole.TEACHER);
        userRepository.save(teacher);
    }

    private void seedStudentAccounts() {
        studentRepository.findAll().forEach(student -> {
            if (userRepository.existsByStudentId(student.getId())) {
                return;
            }
            SysUser user = new SysUser();
            user.setUsername(student.getStudentNumber());
            user.setPasswordHash(passwordEncoder.encode("student123"));
            user.setRole(UserRole.STUDENT);
            user.setStudentId(student.getId());
            userRepository.save(user);
        });
    }
}
