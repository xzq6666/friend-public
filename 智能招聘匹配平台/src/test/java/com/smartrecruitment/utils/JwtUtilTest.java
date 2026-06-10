package com.smartrecruitment.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 测试 JWT Token 的生成、解析、验证功能
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 通过反射注入配置值
        ReflectionTestUtils.setField(jwtUtil, "secret", "your_jwt_secret_key_here_make_it_long_and_secure");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24小时
    }

    @Test
    void testGenerateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("1", "testuser");
        assertNotNull(token, "生成的 Token 不应为空");
        assertTrue(token.length() > 0, "Token 长度应大于 0");
    }

    @Test
    void testExtractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("1", "testuser");
        String username = jwtUtil.extractUsername(token);
        assertEquals("testuser", username, "提取的用户名应与生成时一致");
    }

    @Test
    void testExtractExpiration_shouldReturnFutureDate() {
        String token = jwtUtil.generateToken("1", "testuser");
        assertFalse(jwtUtil.isTokenExpired(token), "新生成的 Token 不应过期");
    }

    @Test
    void testIsTokenExpired_withExpiredToken_shouldReturnTrue() {
        // 设置过期时间为 0，使 Token 立即过期
        ReflectionTestUtils.setField(jwtUtil, "expiration", 0L);
        String token = jwtUtil.generateToken("1", "testuser");
        // 等待一毫秒确保过期
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        assertTrue(jwtUtil.isTokenExpired(token), "过期时间为0的 Token 应立即过期");
    }

    @Test
    void testValidateToken_withValidToken_shouldReturnTrue() {
        String token = jwtUtil.generateToken("1", "testuser");
        Boolean result = jwtUtil.validateToken(token, "testuser");
        assertTrue(result, "有效 Token 验证应返回 true");
    }

    @Test
    void testValidateToken_withWrongUsername_shouldReturnFalse() {
        String token = jwtUtil.generateToken("1", "testuser");
        Boolean result = jwtUtil.validateToken(token, "wronguser");
        assertFalse(result, "用户名不匹配时应返回 false");
    }

    @Test
    void testValidateToken_withExpiredToken_shouldReturnFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", 0L);
        String token = jwtUtil.generateToken("1", "testuser");
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        Boolean result = jwtUtil.validateToken(token, "testuser");
        assertFalse(result, "过期 Token 验证应返回 false");
    }

    @Test
    void testGenerateToken_withDifferentUsers_shouldReturnDifferentTokens() {
        String token1 = jwtUtil.generateToken("1", "user1");
        String token2 = jwtUtil.generateToken("2", "user2");
        assertNotEquals(token1, token2, "不同用户生成的 Token 应不同");
    }

    @Test
    void testExtractUsername_fromInvalidToken_shouldThrowException() {
        assertThrows(Exception.class, () -> {
            jwtUtil.extractUsername("invalid.token.string");
        }, "解析非法 Token 应抛出异常");
    }
}
