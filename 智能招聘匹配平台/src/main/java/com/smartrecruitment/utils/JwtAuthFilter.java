package com.smartrecruitment.utils;

import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/uploads/") || uri.startsWith("/api/uploads/")
                || uri.startsWith("/chat-files/") || uri.startsWith("/api/chat-files/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                log.debug("JWT验证: URI={}, username={}", requestURI, username);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (!jwtUtil.isTokenExpired(token)) {
                        User user = userService.findByUsername(username);
                        if (user == null) {
                            log.warn("JWT验证通过但用户不存在: username={}, URI={}", username, requestURI);
                        }
                        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                        if (user != null && user.getUserType() != null) {
                            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType()));
                        }
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(username, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        log.warn("JWT Token 已过期: URI={}, token前20字符={}", requestURI, token.substring(0, Math.min(20, token.length())));
                    }
                } else if (username == null) {
                    log.warn("JWT Token 无法提取用户名: URI={}", requestURI);
                }
            } catch (Exception e) {
                log.error("JWT Token 验证失败: URI={}, error={}, exceptionType={}", requestURI, e.getMessage(), e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        } else if (!isPublicPath(requestURI)) {
            log.debug("请求无Authorization头: URI={}", requestURI);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String uri) {
        // uri 可能包含 context-path (/api) 也可能不包含，两种形式都匹配
        return uri.startsWith("/api/auth/") || uri.startsWith("/auth/")
                || uri.startsWith("/api/uploads/") || uri.startsWith("/uploads/")
                || uri.startsWith("/api/chat-files/") || uri.startsWith("/chat-files/")
                || uri.contains("parse-preview")
                || uri.contains("stats/public");
    }
}
