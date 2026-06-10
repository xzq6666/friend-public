package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.Announcement;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.AnnouncementService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/announcement")
@CrossOrigin
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private UserService userService;

    /**
     * 管理员发布公告
     */
    @PostMapping
    public ResponseEntity<?> publish(@RequestBody Announcement announcement, Authentication auth) {
        User user = checkAdmin(auth);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "需要管理员权限"));
        }

        announcement.setPublisherId(user.getId());
        announcement.setIsActive(1);
        announcementService.publish(announcement);

        return ResponseEntity.ok(Map.of("message", "公告发布成功"));
    }

    /**
     * 获取公告列表（分页）
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetType,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getIsActive, 1);

        if (targetType != null && !targetType.isEmpty()) {
            wrapper.and(w -> w.eq(Announcement::getTargetType, targetType)
                    .or().eq(Announcement::getTargetType, "ALL"));
        } else if (!"ADMIN".equals(user.getUserType())) {
            // 非管理员只能看到发给自己的公告
            wrapper.and(w -> w.eq(Announcement::getTargetType, user.getUserType())
                    .or().eq(Announcement::getTargetType, "ALL"));
        }

        wrapper.orderByDesc(Announcement::getCreateTime);

        IPage<Announcement> pageResult = announcementService.page(new Page<>(page, size), wrapper);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * 获取最新公告（用于侧边栏展示）
     */
    @GetMapping("/latest")
    public ResponseEntity<?> latest(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getIsActive, 1);

        if (!"ADMIN".equals(user.getUserType())) {
            wrapper.and(w -> w.eq(Announcement::getTargetType, user.getUserType())
                    .or().eq(Announcement::getTargetType, "ALL"));
        }

        wrapper.orderByDesc(Announcement::getCreateTime);
        wrapper.last("LIMIT 10");

        List<Announcement> list = announcementService.list(wrapper);
        return ResponseEntity.ok(list);
    }

    /**
     * 修改公告
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Announcement announcement, Authentication auth) {
        User user = checkAdmin(auth);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "需要管理员权限"));
        }

        Announcement existing = announcementService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (announcement.getTitle() != null) existing.setTitle(announcement.getTitle());
        if (announcement.getContent() != null) existing.setContent(announcement.getContent());
        if (announcement.getTargetType() != null) existing.setTargetType(announcement.getTargetType());
        announcementService.updateById(existing);

        return ResponseEntity.ok(Map.of("message", "公告修改成功"));
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        User user = checkAdmin(auth);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "需要管理员权限"));
        }

        announcementService.removeById(id);
        return ResponseEntity.ok(Map.of("message", "公告已删除"));
    }

    private User checkAdmin(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        User user = userService.findByUsername(auth.getName());
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            return null;
        }
        return user;
    }
}
