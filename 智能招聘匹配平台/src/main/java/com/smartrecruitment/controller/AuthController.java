package com.smartrecruitment.controller;

import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.OperationLogService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.utils.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ================================================
 * 认证控制器 - 用户注册与登录
 * ================================================
 *
 * URL 前缀：/auth（放行路径，无需认证）
 *
 * 接口列表：
 *   POST /auth/register   → 用户注册（密码自动加密）
 *   POST /auth/login      → 用户登录（返回 JWT Token）
 *
 * 如需扩展：
 *   - 添加 POST /auth/refresh 刷新 Token
 *   - 添加 POST /auth/logout  登出（清除 Token）
 *   - 添加 POST /auth/forgot  忘记密码（发送验证邮件）
 *   - 添加 POST /auth/reset   重置密码
 *   - 添加 GET /auth/verify?token=xxx 邮箱验证
 *   - 添加验证码功能（图片验证码 / 短信验证码）
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ResumeService resumeService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            boolean success = userService.register(user);
            if (success) {
                // 记录注册日志
                User registeredUser = userService.findByUsername(user.getUsername());
                if (registeredUser != null) {
                    operationLogService.log(
                            registeredUser.getId(),
                            registeredUser.getUsername(),
                            "注册",
                            "用户",
                            "用户注册成功，类型：" + registeredUser.getUserType(),
                            IpUtils.getClientIp(),
                            1
                    );
                    // 发送欢迎通知
                    String welcomeMsg = "EMPLOYER".equals(registeredUser.getUserType())
                            ? "欢迎加入智能招聘匹配平台！您可以发布职位、搜索人才，开始您的招聘之旅。"
                            : "欢迎加入智能招聘匹配平台！完善您的简历，开始探索心仪的工作机会吧。";
                    notificationService.send(registeredUser.getId(), "欢迎加入平台", welcomeMsg, "system");
                }
                return ResponseEntity.ok(Map.of("message", "注册成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "注册失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
            }

            String token = userService.login(username, password);
            if (token != null) {
                User user = userService.findByUsername(username);
                // 记录登录成功日志
                operationLogService.log(
                        user.getId(),
                        user.getUsername(),
                        "登录",
                        "用户",
                        "用户登录成功",
                        IpUtils.getClientIp(),
                        1
                );
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                // 返回用户信息但排除敏感字段
                Map<String, Object> safeUser = new HashMap<>();
                safeUser.put("id", user.getId());
                safeUser.put("username", user.getUsername());
                safeUser.put("userType", user.getUserType());
                safeUser.put("email", user.getEmail());
                safeUser.put("phone", user.getPhone());
                safeUser.put("avatar", user.getAvatar());
                safeUser.put("status", user.getStatus());
                response.put("user", safeUser);
                return ResponseEntity.ok(response);
            } else {
                // 记录登录失败日志
                operationLogService.log(
                        null,
                        username,
                        "登录",
                        "用户",
                        "用户登录失败，用户名或密码错误",
                        IpUtils.getClientIp(),
                        0
                );
                return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "登录失败，请稍后重试"));
        }
    }

    /**
     * 获取公开统计数据（登录页使用）
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getPublicStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("jobCount", jobService.count());
        stats.put("userCount", userService.count());
        stats.put("resumeCount", resumeService.count());
        return ResponseEntity.ok(stats);
    }
}
