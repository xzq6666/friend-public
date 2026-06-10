package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruitment.entity.CompanyInfo;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.CompanyInfoService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 企业信息控制器
 */
@RestController
@RequestMapping("/company")
public class CompanyInfoController {

    @Autowired
    private CompanyInfoService companyInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Value("${upload.local-path:./uploads}")
    private String localUploadPath;

    /**
     * 获取当前用户的企业信息
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyCompany(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }
        
        // 只允许企业用户访问
        if (!"EMPLOYER".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "仅企业用户可访问"));
        }
        
        CompanyInfo company = companyInfoService.getByUserId(currentUser.getId());
        return ResponseEntity.ok(company);
    }

    /**
     * 保存或更新企业信息
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveCompany(@RequestBody CompanyInfo company, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }
        
        // 只允许企业用户访问
        if (!"EMPLOYER".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "仅企业用户可访问"));
        }
        
        // 设置用户ID
        company.setUserId(currentUser.getId());

        // 检查是否需要重新提交认证
        CompanyInfo existing = companyInfoService.getByUserId(currentUser.getId());
        if (existing != null && existing.getVerified() == 3) {
            // 认证被拒绝，允许重新提交
            company.setVerified(1); // 待审核
            company.setVerifyRemark(null);
            company.setVerifyTime(null);
        } else if (company.getBusinessLicense() != null && !company.getBusinessLicense().isEmpty()) {
            // 新提交认证
            company.setVerified(1); // 待审核
        }
        
        CompanyInfo saved = companyInfoService.saveOrUpdateCompany(company);

        // 提交认证时通知所有管理员
        if (saved.getVerified() != null && saved.getVerified() == 1) {
            List<User> admins = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getUserType, "ADMIN"));
            List<Long> adminIds = admins.stream()
                .map(User::getId)
                .collect(Collectors.toList());
            if (!adminIds.isEmpty()) {
                notificationService.sendBatch(adminIds,
                    "新企业认证申请",
                    "企业「" + saved.getCompanyName() + "」提交了认证申请，请及时审核。",
                    "system");
            }
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * 上传营业执照
     */
    @PostMapping("/upload/license")
    public ResponseEntity<?> uploadLicense(@RequestParam("file") MultipartFile file, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "只能上传图片文件"));
        }

        // 验证文件大小（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "图片大小不能超过5MB"));
        }

        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = "license_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

            Path dir = Paths.get(localUploadPath, "license");
            Files.createDirectories(dir);
            Path fullPath = dir.resolve(filename);
            Files.write(fullPath, file.getBytes());

            String url = "/api/uploads/license/" + filename;
            return ResponseEntity.ok(Map.of("url", url, "fileUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 根据用户ID获取企业信息（公开接口，求职者可查看）
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCompanyByUserId(@PathVariable Long userId) {
        CompanyInfo company = companyInfoService.getByUserId(userId);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 隐藏敏感信息（营业执照URL等）
        company.setBusinessLicense(null);
        
        return ResponseEntity.ok(company);
    }
}
