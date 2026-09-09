package com.example.app.security;

import com.example.app.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具类（SPEC-identity）
 * 负责 token 的签发与解析校验；claims：sub=用户ID、username、role。
 * 密钥来自配置 app.jwt.secret，过期时长来自 app.jwt.expiration-hours。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-hours:8}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = Duration.ofHours(expirationHours).toMillis();
    }

    /**
     * 签发 JWT
     * @param userId 用户ID（写入 sub）
     * @param username 用户名
     * @param role 角色
     * @return 紧凑序列化的 JWT 字符串
     */
    public String generateToken(Long userId, String username, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 JWT，非法/过期抛出 JwtException
     * @param token 不含 "Bearer " 前缀的 token
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
