package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.JobApplicationService;
import com.smartrecruitment.service.InterviewService;
import com.smartrecruitment.service.MatchRecordService;
import com.smartrecruitment.entity.NotificationPreference;
import com.smartrecruitment.mapper.NotificationPreferenceMapper;
import com.smartrecruitment.service.FileUploadService;
import com.smartrecruitment.service.OperationLogService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.utils.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================
 * 用户管理控制器 - 用户信息查询与管理
 * ================================================
 *
 * URL 前缀：/user（需登录认证）
 *
 * 接口列表：
 *   GET  /user/profile       → 获取当前登录用户信息
 *   GET  /user/list          → 用户列表（分页+关键词搜索）
 *   GET  /user/{id}          → 获取指定用户详情
 *   PUT  /user/{id}/status   → 更新用户状态（启用/禁用）
 *
 * 如需扩展：
 *   - 添加 PUT /user/profile 更新用户资料
 *   - 添加 PUT /user/password 修改密码
 *   - 添加 POST /user/admin 创建管理员账号
 *   - 添加 DELETE /user/{id} 删除用户
 *   - 添加 GET /user/export 导出用户数据
 *   - 添加角色权限控制（管理员 / 普通用户）
 *   - 添加用户操作日志记录
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private MatchRecordService matchRecordService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private JobService jobService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private NotificationPreferenceMapper notificationPreferenceMapper;

    @Value("${upload.local-path:./uploads}")
    private String localUploadPath;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        String username = auth.getName();
        User user = userService.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取个人中心的统计数据
     */
    @GetMapping("/profile/stats")
    public ResponseEntity<?> getProfileStats(Authentication auth) {
        String username = auth.getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Map<String, Object> stats = new HashMap<>();

        if ("EMPLOYER".equals(user.getUserType())) {
            // 企业用户统计数据
            // count1: 发布职位数
            long jobCount = jobService.lambdaQuery()
                    .eq(Job::getEmployerId, user.getId())
                    .count();
            stats.put("count1", jobCount);

            // count2: 收到简历数（投递到该企业职位的数量）
            long receivedCount = jobApplicationService.lambdaQuery()
                    .inSql(JobApplication::getJobId, 
                        "SELECT id FROM job WHERE employer_id = " + user.getId())
                    .count();
            stats.put("count2", receivedCount);

            // count3: 面试邀请数
            List<Map<String, Object>> interviews = interviewService.getEmployerInterviews(user.getId());
            stats.put("count3", interviews.size());

            // count4: 已录用数
            long hiredCount = jobApplicationService.lambdaQuery()
                    .inSql(JobApplication::getJobId,
                        "SELECT id FROM job WHERE employer_id = " + user.getId())
                    .eq(JobApplication::getStatus, 3) // 3 = 已录用
                    .count();
            stats.put("count4", hiredCount);
        } else if ("ADMIN".equals(user.getUserType())) {
            // 管理员统计数据
            // count1: 管理职位总数
            long jobCount = jobService.count();
            stats.put("count1", jobCount);

            // count2: 管理简历总数
            long resumeCount = resumeService.count();
            stats.put("count2", resumeCount);

            // count3: 平台用户总数
            long userCount = userService.count();
            stats.put("count3", userCount);

            // count4: 总投递记录数
            long applicationCount = jobApplicationService.count();
            stats.put("count4", applicationCount);
        } else {
            // 求职者统计数据
            // count1: 我的简历数
            long resumeCount = resumeService.lambdaQuery()
                    .eq(Resume::getUserId, user.getId())
                    .count();
            stats.put("count1", resumeCount);

            // count2: 投递职位数
            List<Map<String, Object>> applications = jobApplicationService.getUserApplications(user.getId());
            stats.put("count2", applications.size());

            // count3: 匹配成功数
            // 取第一条简历
            Resume firstResume = resumeService.lambdaQuery()
                    .eq(Resume::getUserId, user.getId())
                    .last("LIMIT 1")
                    .one();
            if (firstResume != null) {
                List<Map<String, Object>> matches = matchRecordService.getMatchRecordsWithJob(firstResume.getId());
                long matchSuccess = matches.stream()
                        .filter(m -> {
                            Integer status = (Integer) m.get("status");
                            return status != null && status >= 2;
                        })
                        .count();
                stats.put("count3", matchSuccess);
            } else {
                stats.put("count3", 0);
            }

            // count4: 被查看数
            // 这里可以用投递记录中被企业处理过的数量来表示
            long viewedCount = applications.stream()
                    .filter(a -> {
                        Integer status = (Integer) a.get("status");
                        return status != null && status >= 1; // 已查看
                    })
                    .count();
            stats.put("count4", viewedCount);
        }

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        // 所有登录用户都可以查看用户列表
        IPage<User> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        pageResult = userService.page(pageResult, wrapper);
        // 仅隐藏密码，保留邮箱和手机号供管理员查看
        pageResult.getRecords().forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id, Authentication auth) {
        // 所有登录用户都可以查看用户详情
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // 创建副本避免修改MyBatis一级缓存中的实体
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setUserType(user.getUserType());
        safeUser.setAvatar(user.getAvatar());
        safeUser.setStatus(user.getStatus());
        safeUser.setCreateTime(user.getCreateTime());
        // 只有管理员或本人可以查看邮箱和手机号
        if (auth != null && !"anonymousUser".equals(auth.getName())) {
            User currentUser = userService.findByUsername(auth.getName());
            if (currentUser != null && (currentUser.getId().equals(id) || "ADMIN".equals(currentUser.getUserType()))) {
                safeUser.setEmail(user.getEmail());
                safeUser.setPhone(user.getPhone());
            }
        }
        return ResponseEntity.ok(safeUser);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body, Authentication auth) {
        // 检查是否为管理员
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Integer oldStatus = user.getStatus();
        user.setStatus(body.getOrDefault("status", 1));
        userService.updateById(user);

        // 记录日志
        operationLogService.log(
                currentUser.getId(),
                currentUser.getUsername(),
                "更新",
                "用户",
                String.format("%s管理员%s用户 [%s]", oldStatus == 1 ? "禁用" : "启用", currentUser.getUsername(), user.getUsername()),
                IpUtils.getClientIp(),
                1
        );
        return ResponseEntity.ok(Map.of("message", "状态更新成功"));
    }

    /**
     * 修改用户密码（仅管理员）
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        // 检查是否为管理员
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码不能为空"));
        }

        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);

        // 记录日志
        operationLogService.log(
                currentUser.getId(),
                currentUser.getUsername(),
                "更新",
                "用户",
                String.format("管理员重置了用户 [%s] 的密码", user.getUsername()),
                IpUtils.getClientIp(),
                1
        );
        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    /**
     * 删除用户（仅管理员）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication auth) {
        // 检查是否为管理员
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        // 不能删除自己
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "不能删除自己的账号"));
        }

        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        userService.removeById(id);

        // 记录日志
        operationLogService.log(
                currentUser.getId(),
                currentUser.getUsername(),
                "删除",
                "用户",
                String.format("管理员删除了用户 [%s]", user.getUsername()),
                IpUtils.getClientIp(),
                1
        );
        return ResponseEntity.ok(Map.of("message", "用户删除成功"));
    }

    /**
     * 修改当前用户密码
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "原密码不能为空"));
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码不能为空"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码长度不能少于6位"));
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "原密码不正确"));
        }

        // 更新密码
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(currentUser);

        // 记录日志
        operationLogService.log(
                currentUser.getId(),
                currentUser.getUsername(),
                "更新",
                "用户",
                "用户修改了登录密码",
                IpUtils.getClientIp(),
                1
        );
        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, Authentication auth) {
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        // 更新邮箱
        String email = body.get("email");
        if (email != null && !email.isEmpty()) {
            // 检查邮箱是否已被其他用户使用
            User existingUser = userService.lambdaQuery()
                    .eq(User::getEmail, email)
                    .ne(User::getId, currentUser.getId())
                    .one();
            if (existingUser != null) {
                return ResponseEntity.badRequest().body(Map.of("error", "该邮箱已被其他用户使用"));
            }
            currentUser.setEmail(email);
        }

        // 更新手机号
        String phone = body.get("phone");
        if (phone != null) {
            currentUser.setPhone(phone);
        }

        userService.updateById(currentUser);

        // 记录日志
        operationLogService.log(
                currentUser.getId(),
                currentUser.getUsername(),
                "更新",
                "用户",
                "用户更新了个人资料",
                IpUtils.getClientIp(),
                1
        );

        // 返回更新后的用户信息（隐藏密码）
        currentUser.setPassword(null);
        return ResponseEntity.ok(currentUser);
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication auth) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择要上传的图片"));
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持图片文件"));
        }

        // 校验文件大小 (最大 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "图片大小不能超过 5MB"));
        }

        String username = auth.getName();
        User currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        try {
            // 使用 FileUploadService 上传，但存到 avatar 目录
            String avatarUrl = uploadAvatarFile(file);
            currentUser.setAvatar(avatarUrl);
            userService.updateById(currentUser);

            // 记录日志
            operationLogService.log(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    "更新",
                    "用户",
                    "用户上传了新头像",
                    IpUtils.getClientIp(),
                    1
            );

            return ResponseEntity.ok(Map.of("avatar", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取头像文件（直接输出图片）
     */
    @GetMapping("/avatar/{filename}")
    public void getAvatar(@PathVariable String filename, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        Path filePath = Paths.get(localUploadPath).toAbsolutePath().normalize().resolve("avatar").resolve(filename);
        java.io.File file = filePath.toFile();
        System.out.println("[Avatar] 请求头像: " + filename + ", 路径: " + filePath.toAbsolutePath() + ", 存在: " + file.exists());
        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Avatar not found: " + filePath.toAbsolutePath());
            return;
        }
        String contentType = java.nio.file.Files.probeContentType(filePath);
        response.setContentType(contentType != null ? contentType : "image/png");
        response.setHeader("Cache-Control", "max-age=86400");
        java.nio.file.Files.copy(filePath, response.getOutputStream());
    }

    /**
     * 上传头像文件（存储到 avatar 目录）
     */
    private String uploadAvatarFile(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        String relativePath = "avatar/" + filename;

        Path basePath = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(relativePath);
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, file.getBytes());

        return "/uploads/avatar/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "png";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取通知偏好设置
     */
    @GetMapping("/notification-prefs")
    public ResponseEntity<?> getNotificationPrefs(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        NotificationPreference prefs = notificationPreferenceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationPreference>()
                        .eq(NotificationPreference::getUserId, currentUser.getId())
        );

        if (prefs == null) {
            // 返回默认设置
            prefs = new NotificationPreference();
            prefs.setUserId(currentUser.getId());
            prefs.setEmailNotify(1);
            prefs.setSmsNotify(1);
            prefs.setJobMatchNotify(1);
            prefs.setInterviewNotify(1);
            prefs.setSystemNotify(1);
        }

        return ResponseEntity.ok(prefs);
    }

    /**
     * 更新通知偏好设置
     */
    @PutMapping("/notification-prefs")
    public ResponseEntity<?> updateNotificationPrefs(@RequestBody Map<String, Object> body, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        NotificationPreference prefs = notificationPreferenceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationPreference>()
                        .eq(NotificationPreference::getUserId, currentUser.getId())
        );

        if (prefs == null) {
            prefs = new NotificationPreference();
            prefs.setUserId(currentUser.getId());
            prefs.setEmailNotify(1);
            prefs.setSmsNotify(1);
            prefs.setJobMatchNotify(1);
            prefs.setInterviewNotify(1);
            prefs.setSystemNotify(1);
        }

        // 辅助方法：安全地将 Object 转为 Integer (0/1)
        java.util.function.Function<Object, Integer> toInt = (val) -> {
            if (val == null) return null;
            if (val instanceof Boolean) return (Boolean) val ? 1 : 0;
            if (val instanceof Number) return ((Number) val).intValue();
            return Integer.parseInt(val.toString());
        };

        // 更新字段
        if (body.containsKey("emailNotify")) {
            prefs.setEmailNotify(toInt.apply(body.get("emailNotify")));
        }
        if (body.containsKey("smsNotify")) {
            prefs.setSmsNotify(toInt.apply(body.get("smsNotify")));
        }
        if (body.containsKey("jobMatchNotify")) {
            prefs.setJobMatchNotify(toInt.apply(body.get("jobMatchNotify")));
        }
        if (body.containsKey("interviewNotify")) {
            prefs.setInterviewNotify(toInt.apply(body.get("interviewNotify")));
        }
        if (body.containsKey("systemNotify")) {
            prefs.setSystemNotify(toInt.apply(body.get("systemNotify")));
        }

        if (prefs.getId() == null) {
            notificationPreferenceMapper.insert(prefs);
        } else {
            notificationPreferenceMapper.updateById(prefs);
        }

        // 记录日志
        operationLogService.log(
                currentUser.getId(),
                currentUser.getUsername(),
                "更新",
                "用户",
                "用户更新了通知偏好设置",
                IpUtils.getClientIp(),
                1
        );

        return ResponseEntity.ok(prefs);
    }
}
