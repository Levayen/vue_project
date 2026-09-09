package com.example.app.security;

import com.example.app.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtUtil 单元测试（SPEC-identity）
 */
class JwtUtilTest {

    private static final String SECRET = "testSecretKeyForJwtUnitTestMustBeAtLeast32BytesLong0123";
    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 8);

    @Test
    void generateAndParseRoundTrip() {
        String token = jwtUtil.generateToken(7L, "admin", UserRole.ADMIN);

        Claims claims = jwtUtil.parse(token);
        assertEquals("7", claims.getSubject());
        assertEquals("admin", claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void expiredTokenRejected() throws InterruptedException {
        JwtUtil expiredUtil = new JwtUtil(SECRET, 0);
        String token = expiredUtil.generateToken(1L, "u", UserRole.STUDENT);

        Thread.sleep(1100);
        assertThrows(JwtException.class, () -> expiredUtil.parse(token));
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwtUtil.generateToken(1L, "u", UserRole.STUDENT);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThrows(JwtException.class, () -> jwtUtil.parse(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretRejected() {
        JwtUtil other = new JwtUtil("anotherSecretKeyAlsoAtLeast32BytesLong0123456789", 8);
        String token = other.generateToken(1L, "u", UserRole.ADMIN);

        assertThrows(JwtException.class, () -> jwtUtil.parse(token));
    }
}
