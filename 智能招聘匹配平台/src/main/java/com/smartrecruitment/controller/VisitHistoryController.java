package com.smartrecruitment.controller;

import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.service.UserPrivacySettingsService;
import com.smartrecruitment.service.VisitHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ================================================
 * 访问历史控制器
 * ================================================
 *
 * URL 前缀：/visit-history
 *
 * 接口列表：
 *   GET    /visit-history/visitors  → 查看谁访问了我（分页）
 *   GET    /visit-history/stats     → 获取访问统计
 *   GET    /visit-history/privacy   → 获取隐私设置
 *   PUT    /visit-history/privacy   → 更新隐私设置
 */
@RestController
@RequestMapping("/visit-history")
@CrossOrigin
public class VisitHistoryController {

    @Autowired
    private VisitHistoryService visitHistoryService;

    @Autowired
    private UserPrivacySettingsService privacySettingsService;

    @Autowired
    private UserService userService;

    /**
     * 查看谁访问了我
     * @param targetType 过滤类型: null=全部, 1=简历/主页, 2=职位
     */
    @GetMapping("/visitors")
    public ResponseEntity<?> getMyVisitors(
            @RequestParam(required = false) Integer targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
            }

            Map<String, Object> result = visitHistoryService.getMyVisitors(user.getId(), targetType, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取访问统计
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getVisitStats(
            @RequestParam(required = false) Integer targetType,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
            }

            Map<String, Object> stats = visitHistoryService.getVisitStats(user.getId(), targetType);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前用户的隐私设置
     */
    @GetMapping("/privacy")
    public ResponseEntity<?> getPrivacySettings(Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
            }

            var settings = privacySettingsService.getOrCreateByUserId(user.getId());
            return ResponseEntity.ok(Map.of(
                    "defaultAnonymous", settings.getDefaultAnonymous() == 1,
                    "showVisitHistory", settings.getShowVisitHistory() == 1
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新当前用户的隐私设置
     */
    @PutMapping("/privacy")
    public ResponseEntity<?> updatePrivacySettings(
            @RequestBody Map<String, Boolean> body,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
            }

            privacySettingsService.updateSettings(
                    user.getId(),
                    body.get("defaultAnonymous"),
                    body.get("showVisitHistory")
            );
            return ResponseEntity.ok(Map.of("message", "设置已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
