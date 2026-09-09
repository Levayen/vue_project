package com.example.app.config;

import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * 种子数据初始化（SPEC-identity，M0 最小集）
 * 初始账号安全策略（Issue1 修复）：
 * - 初始密码不硬编码，统一从配置/环境变量读取
 *   （app.security.seed.admin-password / teacher-password / student-password，
 *    对应环境变量 APP_SECURITY_SEED_ADMIN_PASSWORD / _TEACHER_PASSWORD / _STUDENT_PASSWORD）；
 * - 未配置时为每个种子账号随机生成强密码，并打印到启动日志（WARN），仅限首次启动可见；
 * - 所有种子账号 must_change_password=true，首次登录强制修改密码。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();

    private final SysUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed.admin-password:}")
    private String adminSeedPassword;

    @Value("${app.security.seed.teacher-password:}")
    private String teacherSeedPassword;

    @Value("${app.security.seed.student-password:}")
    private String studentSeedPassword;

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
        String password = resolvePassword(adminSeedPassword, "admin");
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setMustChangePassword(true);
        userRepository.save(admin);
    }

    private void seedTeacher() {
        if (userRepository.existsByUsername("teacher")) {
            return;
        }
        String password = resolvePassword(teacherSeedPassword, "teacher");
        SysUser teacher = new SysUser();
        teacher.setUsername("teacher");
        teacher.setPasswordHash(passwordEncoder.encode(password));
        teacher.setRole(UserRole.TEACHER);
        teacher.setMustChangePassword(true);
        userRepository.save(teacher);
    }

    private void seedStudentAccounts() {
        studentRepository.findAll().forEach(student -> {
            if (userRepository.existsByStudentId(student.getId())) {
                return;
            }
            String password = resolvePassword(studentSeedPassword, "student(" + student.getStudentNumber() + ")");
            SysUser user = new SysUser();
            user.setUsername(student.getStudentNumber());
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(UserRole.STUDENT);
            user.setStudentId(student.getId());
            user.setMustChangePassword(true);
            userRepository.save(user);
        });
    }

    /**
     * 解析种子密码：配置/环境变量提供则使用（不打印）；
     * 未提供则随机生成 16 位强密码并 WARN 日志提示（一次性引导凭据）。
     */
    private String resolvePassword(String configured, String accountDesc) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String generated = randomPassword(16);
        log.warn("种子账号[{}]未配置初始密码（环境变量 APP_SECURITY_SEED_*），"
                + "已随机生成一次性初始密码：{} —— 请尽快登录并修改，切勿在生产环境沿用。",
                accountDesc, generated);
        return generated;
    }

    /**
     * 生成强随机密码（大小写字母+数字，至少包含 2 位数字）
     */
    static String randomPassword(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        // 保证至少 2 位数字
        for (int i = 0; i < 2; i++) {
            int pos = RANDOM.nextInt(length);
            chars[pos] = Character.forDigit(RANDOM.nextInt(10), 10);
        }
        return new String(chars);
    }
}
