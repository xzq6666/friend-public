package com.smartrecruitment.controller;

import com.smartrecruitment.entity.ResumeVersion;
import com.smartrecruitment.service.ResumeVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历版本控制器
 */
@RestController
@RequestMapping("/api/resume-version")
public class ResumeVersionController {
    
    @Autowired
    private ResumeVersionService versionService;
    
    @Autowired
    private com.smartrecruitment.service.UserService userService;
    
    /**
     * 从Authentication获取用户ID
     */
    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            throw new IllegalArgumentException("请先登录");
        }
        String username = auth.getName();
        com.smartrecruitment.entity.User user = userService.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user.getId();
    }
    
    /**
     * 创建新版本
     */
    @PostMapping("/{resumeId}")
    public ResponseEntity<?> createVersion(@PathVariable Long resumeId,
                                           @RequestParam(required = false) String tag,
                                           @RequestParam(required = false) String note,
                                           Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            
            ResumeVersion version = versionService.createVersion(resumeId, tag, note, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", version);
            result.put("message", "版本创建成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 查询简历的所有版本
     */
    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<?> getVersions(@PathVariable Long resumeId) {
        try {
            List<ResumeVersion> versions = versionService.getVersions(resumeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", versions);
            result.put("count", versions.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取版本详情
     */
    @GetMapping("/{versionId}")
    public ResponseEntity<?> getVersionDetail(@PathVariable Long versionId) {
        try {
            ResumeVersion version = versionService.getVersionDetail(versionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", version);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 恢复到指定版本
     */
    @PostMapping("/{versionId}/restore")
    public ResponseEntity<?> restoreVersion(@PathVariable Long versionId, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            
            boolean success = versionService.restoreToVersion(versionId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "恢复成功" : "恢复失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 对比两个版本
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compareVersions(@RequestParam Long v1, @RequestParam Long v2) {
        try {
            Map<String, Object> comparison = versionService.compareVersions(v1, v2);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", comparison);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 删除版本
     */
    @DeleteMapping("/{versionId}")
    public ResponseEntity<?> deleteVersion(@PathVariable Long versionId, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            
            boolean success = versionService.deleteVersion(versionId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
