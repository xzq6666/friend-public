package com.smartrecruitment.controller;

import com.smartrecruitment.service.JobApplicationBoardService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 求职进度看板控制器
 */
@RestController
@RequestMapping("/api/board")
@CrossOrigin
public class JobApplicationBoardController {
    
    @Autowired
    private JobApplicationBoardService boardService;
    
    @Autowired
    private UserService userService;
    
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
     * 获取看板数据
     */
    @GetMapping("/data")
    public ResponseEntity<?> getBoardData(Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            Map<String, Object> boardData = boardService.getBoardData(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", boardData);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 更新申请状态（拖拽卡片）
     */
    @PutMapping("/application/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            boolean success = boardService.updateApplicationStatus(id, status, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "状态更新成功" : "状态更新失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            Map<String, Object> stats = boardService.getStatistics(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", stats);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
