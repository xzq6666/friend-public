package com.smartrecruitment;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码测试工具
 */
public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 测试"123456"密码的哈希
        String password = "123456";
        String encoded = encoder.encode(password);
        
        System.out.println("=== 密码测试 ===");
        System.out.println("原始密码: " + password);
        System.out.println("新生成的BCrypt哈希: " + encoded);
        
        // 验证
        boolean matches = encoder.matches(password, encoded);
        System.out.println("新哈希匹配结果: " + matches);
        
        // 测试旧的哈希值
        String oldHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        boolean oldMatches = encoder.matches(password, oldHash);
        System.out.println("\n=== 旧哈希值测试 ===");
        System.out.println("旧哈希: " + oldHash);
        System.out.println("旧哈希匹配123456: " + oldMatches);
        
        // 测试其他常见密码
        System.out.println("\n=== 测试其他常见密码 ===");
        String[] testPasswords = {"password", "admin", "admin123", "12345678", "123"};
        for (String testPwd : testPasswords) {
            boolean match = encoder.matches(testPwd, oldHash);
            System.out.println("旧哈希匹配 " + testPwd + ": " + match);
        }
    }
}
