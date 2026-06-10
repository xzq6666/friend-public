package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.UserMapper;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ================================================
 * 用户服务实现
 * ================================================
 *
 * 功能说明：
 *   - findByUsername：按用户名精确查找用户
 *   - login：验证密码并生成 JWT Token
 *   - register：注册新用户（密码 BCrypt 加密）
 *
 * 扩展提示（对应 UserService 接口中的 TODO）：
 *   - 注册时添加邮箱/用户名唯一性校验
 *   - 登录失败次数限制（防暴力破解）
 *   - 用户状态检查（禁用账户不可登录）
 *   - 发送注册欢迎邮件或短信验证
 *   - 第三方 OAuth 登录（微信/GitHub）
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public User findByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    @Override
    public String login(String username, String password) {
        User user = findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // 检查用户状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new RuntimeException("该账户已被禁用，请联系管理员");
            }
            return jwtUtil.generateToken(user.getId().toString(), user.getUsername());
        }
        return null;
    }

    @Override
    public boolean register(User user) {
        // 校验用户名唯一性
        User existing = findByUsername(user.getUsername());
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 校验邮箱唯一性
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            long emailCount = lambdaQuery().eq(User::getEmail, user.getEmail()).count();
            if (emailCount > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }
        // 限制userType，防止注册为ADMIN
        if (user.getUserType() == null || (!user.getUserType().equals("EMPLOYEE") && !user.getUserType().equals("EMPLOYER"))) {
            user.setUserType("EMPLOYEE");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return save(user);
    }

    @Override
    public long countByType(String userType) {
        return lambdaQuery().eq(User::getUserType, userType).count();
    }

    @Override
    public long countByCreateTime(LocalDateTime createTime) {
        LocalDateTime startOfDay = createTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return lambdaQuery()
                .ge(User::getCreateTime, startOfDay)
                .lt(User::getCreateTime, endOfDay)
                .count();
    }

    @Override
    public long countByDateRange(LocalDateTime start, LocalDateTime end) {
        return lambdaQuery()
                .ge(User::getCreateTime, start)
                .lt(User::getCreateTime, end)
                .count();
    }

    @Override
    public long countActiveUsers(LocalDateTime since) {
        return lambdaQuery()
                .ge(User::getUpdateTime, since)
                .count();
    }
}
