package com.example.app.service;

import com.example.app.dto.LoginResponse;
import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;
import com.example.app.exception.BusinessException;
import com.example.app.repository.SysUserRepository;
import com.example.app.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试（SPEC-identity）
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "testSecretKeyForJwtUnitTestMustBeAtLeast32BytesLong0123";

    @Mock
    private SysUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, new JwtUtil(SECRET, 8), passwordEncoder);
    }

    private SysUser user(String username, UserRole role, boolean enabled) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    void loginSuccessReturnsToken() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin", UserRole.ADMIN, true)));
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);

        LoginResponse response = authService.login("admin", "admin123");

        assertNotNull(response.token());
        assertEquals("admin", response.user().username());
        assertEquals(UserRole.ADMIN, response.user().role());
    }

    @Test
    void loginWithWrongPasswordRejected() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin", UserRole.ADMIN, true)));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login("admin", "wrong"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void loginWithUnknownUserRejected() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login("ghost", "x"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void loginWithDisabledAccountRejected() {
        when(userRepository.findByUsername("teacher")).thenReturn(Optional.of(user("teacher", UserRole.TEACHER, false)));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login("teacher", "teacher123"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("账号已停用，请联系管理员", ex.getMessage());
    }

    @Test
    void currentUserReturnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user("admin", UserRole.ADMIN, true)));

        assertEquals("admin", authService.currentUser(1L).username());
    }
}
