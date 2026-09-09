package com.example.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataInitializer 随机种子密码生成器测试（Issue1）
 */
class DataInitializerTest {

    @Test
    void randomPasswordMeetsStrength() {
        for (int i = 0; i < 50; i++) {
            String pwd = DataInitializer.randomPassword(16);
            assertEquals(16, pwd.length(), "密码长度应为 16");
            assertTrue(pwd.chars().anyMatch(Character::isDigit), "应至少包含数字");
            assertTrue(pwd.chars().anyMatch(Character::isLetter), "应至少包含字母");
        }
    }

    @Test
    void randomPasswordsAreDistinct() {
        String a = DataInitializer.randomPassword(16);
        String b = DataInitializer.randomPassword(16);
        assertNotEquals(a, b, "两次生成的随机密码不应相同");
    }
}
