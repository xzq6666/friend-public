package com.smartrecruitment.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ================================================
 * JWT 工具类 - Token 生成与验证
 * ================================================
 *
 * 功能说明：
 *   - generateToken：为用户生成 JWT Token（含用户ID和用户名）
 *   - extractUsername：从 Token 中提取用户名
 *   - isTokenExpired：检查 Token 是否过期
 *   - validateToken：验证 Token 是否有效
 *
 * Token 结构：
 *   - subject: 用户ID
 *   - claims.username: 用户名
 *   - issuedAt: 签发时间
 *   - expiration: 过期时间（默认24小时）
 *
 * 如需扩展：
 *   - 在 claims 中添加更多用户信息（角色、权限等）
 *   - 支持 Token 刷新（Refresh Token 机制）
 *   - 添加 Token 黑名单（注销后使 Token 失效）
 *   - 支持多设备登录限制
 *   - 添加记住我功能（延长过期时间）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 确保密钥至少 512 位（64字节）以支持 HS512
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 64));
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String subject, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        return createToken(claims, subject);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}
