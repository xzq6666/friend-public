package com.smartrecruitment.controller;

import com.smartrecruitment.entity.Interview;
import com.smartrecruitment.entity.InterviewChat;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.InterviewChatMapper;
import com.smartrecruitment.service.InterviewChatService;
import com.smartrecruitment.service.InterviewService;
import com.smartrecruitment.service.SensitiveWordService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/interview-chat")
@CrossOrigin
public class InterviewChatController {

    @Autowired
    private InterviewChatService chatService;

    @Autowired
    private InterviewChatMapper chatMapper;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /**
     * 发送消息
     */
    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Long interviewId = Long.valueOf(body.get("interviewId").toString());
            String content = body.get("content").toString();
            Integer messageType = body.get("messageType") != null ?
                    Integer.valueOf(body.get("messageType").toString()) : 1;

            // 敏感词检测（字典匹配，快速响应）
            List<String> sensitiveWords = sensitiveWordService.checkSensitiveWords(content);
            if (!sensitiveWords.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "消息包含敏感词，无法发送",
                        "sensitiveWords", sensitiveWords
                ));
            }

            // 验证面试权限（只有面试相关的双方才能聊天）
            Interview interview = interviewService.getById(interviewId);
            if (interview == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "面试不存在"));
            }
            if (!interview.getUserId().equals(user.getId()) && !interview.getEmployerId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权限访问此面试"));
            }

            // 确定发送者类型：1-企业，2-求职者
            Integer senderType = interview.getEmployerId().equals(user.getId()) ? 1 : 2;

            InterviewChat chat = chatService.sendMessage(interviewId, user.getId(), senderType, content, messageType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", chat.getId());
            result.put("content", chat.getContent());
            result.put("messageType", chat.getMessageType());
            result.put("senderId", chat.getSenderId());
            result.put("senderType", chat.getSenderType());
            result.put("senderName", user.getUsername());
            result.put("senderAvatar", user.getAvatar());
            result.put("sender_user_type", user.getUserType());
            result.put("createTime", chat.getCreateTime());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取聊天记录
 * 这是一个GET请求方法，用于获取指定面试ID的聊天记录
 * @param interviewId 面试ID，通过路径变量传递
     *
 * @param auth 认证信息，用于验证用户登录状态
 * @return 返回 ResponseEntity<?> 类型，可能包含聊天记录或错误信息
     */
    @GetMapping("/{interviewId}")
    public ResponseEntity<?> getMessages(@PathVariable Long interviewId, Authentication auth) {
        try {
        // 检查用户是否已登录
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
        // 根据用户名查找用户
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            // 验证面试权限 - 检查面试是否存在以及用户是否有权限访问
            Interview interview = interviewService.getById(interviewId);
            if (interview == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "面试不存在"));
            }
        // 检查当前用户是否是面试的参与者（用户或雇主）
            if (!interview.getUserId().equals(user.getId()) && !interview.getEmployerId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权限访问此面试"));
            }

        // 获取并返回聊天记录
            List<Map<String, Object>> messages = chatService.getChatMessages(interviewId, user.getId());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
        // 捕获并返回异常信息
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/{interviewId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long interviewId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            boolean success = chatService.markMessagesAsRead(interviewId, user.getId());
            return ResponseEntity.ok(Map.of("success", success));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/{interviewId}/unread")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long interviewId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            int count = chatService.getUnreadCount(interviewId, user.getId());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除聊天记录（逻辑删除：仅对当前用户隐藏，不影响对方）
     */
    @DeleteMapping("/{interviewId}")
    public ResponseEntity<?> deleteChat(@PathVariable Long interviewId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            // 验证面试权限
            Interview interview = interviewService.getById(interviewId);
            if (interview == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "记录不存在"));
            }
            if (!interview.getUserId().equals(user.getId()) && !interview.getEmployerId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权限删除"));
            }

            // 逻辑删除：根据当前用户类型更新对应的删除标记
            if (interview.getUserId().equals(user.getId())) {
                interview.setDeletedByUser(1);
            } else {
                interview.setDeletedByEmployer(1);
            }
            interviewService.updateById(interview);
            
            return ResponseEntity.ok(Map.of("message", "已从列表中移除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 清除聊天历史（逻辑清除：仅对当前用户隐藏消息，不影响对方）
     */
    @DeleteMapping("/{interviewId}/messages")
    public ResponseEntity<?> clearMessages(@PathVariable Long interviewId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            // 验证面试权限
            Interview interview = interviewService.getById(interviewId);
            if (interview == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "记录不存在"));
            }
            if (!interview.getUserId().equals(user.getId()) && !interview.getEmployerId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
            }

            // 逻辑清除：根据当前用户类型更新对应的删除标记
            String updateField = interview.getUserId().equals(user.getId()) ? "deleted_by_user" : "deleted_by_employer";
            chatMapper.update(null, 
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InterviewChat>()
                    .eq(InterviewChat::getInterviewId, interviewId)
                    .setSql(updateField + " = 1")
            );
            
            return ResponseEntity.ok(Map.of("message", "已清除聊天历史"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 上传文件（图片、文档、简历等）
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("interviewId") Long interviewId,
            @RequestParam(value = "messageType", defaultValue = "2") Integer messageType,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            // 验证面试权限
            Interview interview = interviewService.getById(interviewId);
            if (interview == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "面试不存在"));
            }
            if (!interview.getUserId().equals(user.getId()) && !interview.getEmployerId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权限访问此面试"));
            }

            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
            }

            // 文件大小限制：10MB
            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件大小不能超过10MB"));
            }

            // 文件类型验证
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // 允许的文件类型
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
                                         ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
                                         ".txt", ".zip", ".rar"};
            boolean allowed = false;
            for (String ext : allowedExtensions) {
                if (extension.equals(ext)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件类型"));
            }

            // 保存文件
            String uploadDir = "uploads/chat/" + interviewId + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(uploadDir + uniqueFilename);
            Files.write(filePath, file.getBytes());

            // 确定发送者类型
            Integer senderType = interview.getEmployerId().equals(user.getId()) ? 1 : 2;

            // 创建文件消息
            String fileUrl = "/api/chat-files/" + interviewId + "/" + uniqueFilename;
            InterviewChat chat = chatService.sendFileMessage(interviewId, user.getId(), senderType,
                    originalFilename, messageType, fileUrl, originalFilename);
            
            // 设置文件URL（需要数据库支持file_url字段）
            Map<String, Object> result = new HashMap<>();
            result.put("id", chat.getId());
            result.put("content", originalFilename);
            result.put("messageType", messageType);
            result.put("fileUrl", fileUrl);
            result.put("fileName", originalFilename);
            result.put("senderId", chat.getSenderId());
            result.put("senderType", chat.getSenderType());
            result.put("senderName", user.getUsername());
            result.put("sender_user_type", user.getUserType());
            result.put("createTime", chat.getCreateTime());
            
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件保存失败：" + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
