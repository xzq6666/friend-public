package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.Notification;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.NotificationMapper;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ================================================
 * 通知控制器
 * ================================================
 *
 * URL 前缀：/notification（需登录认证）
 *
 * 接口列表：
 *   GET  /notification/unread-count  → 获取未读通知数量
 *   GET  /notification               → 获取通知列表（分页）
 *   PUT  /notification/{id}/read     → 标记单条通知为已读
 *   PUT  /notification/read-all      → 标记所有通知为已读
 */
@RestController
@RequestMapping("/notification")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, user.getId())
                        .eq(Notification::getIsRead, 0)
        );

        return ResponseEntity.ok(Map.of("count", count != null ? count : 0));
    }

    /**
     * 获取通知列表
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        IPage<Notification> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, user.getId())
                .orderByDesc(Notification::getCreateTime);
        pageResult = notificationMapper.selectPage(pageResult, wrapper);

        return ResponseEntity.ok(pageResult);
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        // 只能标记自己的通知
        if (!notification.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作此通知"));
        }

        notification.setIsRead(1);
        notificationMapper.updateById(notification);

        return ResponseEntity.ok(Map.of("message", "已标记为已读"));
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Notification updateEntity = new Notification();
        updateEntity.setIsRead(1);

        notificationMapper.update(updateEntity,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, user.getId())
                        .eq(Notification::getIsRead, 0)
        );

        return ResponseEntity.ok(Map.of("message", "已全部标记为已读"));
    }

    /**
     * 删除单条通知
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        boolean deleted = notificationService.deleteNotification(id, user.getId());
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "删除失败，通知不存在或无权操作"));
    }
}
