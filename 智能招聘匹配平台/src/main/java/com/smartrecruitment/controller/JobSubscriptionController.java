package com.smartrecruitment.controller;

import com.smartrecruitment.entity.JobSubscription;
import com.smartrecruitment.service.JobSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 职位订阅控制器
 */
@RestController
@RequestMapping("/api/subscription")
public class JobSubscriptionController {
    
    @Autowired
    private JobSubscriptionService subscriptionService;
    
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
     * 创建订阅
     */
    @PostMapping
    public ResponseEntity<?> createSubscription(@RequestBody JobSubscription subscription, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            subscription.setUserId(userId);
            JobSubscription created = subscriptionService.createSubscription(subscription);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", created);
            result.put("message", "订阅创建成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 更新订阅
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubscription(@PathVariable Long id, 
                                                @RequestBody JobSubscription subscription,
                                                Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            subscription.setId(id);
            subscription.setUserId(userId);
            
            boolean success = subscriptionService.updateSubscription(subscription);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 删除订阅
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubscription(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            boolean success = subscriptionService.deleteSubscription(id, userId);
            
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
    
    /**
     * 查询用户的订阅列表
     */
    @GetMapping("/my-subscriptions")
    public ResponseEntity<?> getMySubscriptions(Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            List<JobSubscription> subscriptions = subscriptionService.getUserSubscriptions(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", subscriptions);
            result.put("count", subscriptions.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 激活/停用订阅
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleSubscription(@PathVariable Long id,
                                                @RequestParam Integer isActive,
                                                Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            boolean success = subscriptionService.toggleSubscription(id, userId, isActive);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? (isActive == 1 ? "已激活" : "已停用") : "操作失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 手动触发订阅匹配
     */
    @PostMapping("/{id}/trigger-match")
    public ResponseEntity<?> triggerMatch(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            Map<String, Object> result = subscriptionService.manualTriggerMatch(id, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取订阅的推送历史
     */
    @GetMapping("/{id}/push-history")
    public ResponseEntity<?> getPushHistory(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            List<Map<String, Object>> history = subscriptionService.getPushHistory(id, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", history);
            result.put("count", history.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
