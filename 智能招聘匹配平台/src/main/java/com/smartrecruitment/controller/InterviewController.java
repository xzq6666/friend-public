package com.smartrecruitment.controller;

import com.smartrecruitment.entity.Interview;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.InterviewService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interview")
@CrossOrigin
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;
    
    private static final DateTimeFormatter[] TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };
    
    private LocalDateTime parseTime(String timeStr) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(timeStr, formatter);
            } catch (Exception e) {
                // 尝试下一种格式
            }
        }
        throw new IllegalArgumentException("无法解析时间格式: " + timeStr);
    }

    /**
     * 发送面试邀请（企业操作）
     */
    @PostMapping
    public ResponseEntity<?> createInterview(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Long applicationId = body.get("applicationId") != null ? Long.valueOf(body.get("applicationId").toString()) : null;
            Long jobId = body.get("jobId") != null ? Long.valueOf(body.get("jobId").toString()) : null;
            Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
            Long resumeId = body.get("resumeId") != null ? Long.valueOf(body.get("resumeId").toString()) : null;
            Boolean directChat = body.get("directChat") != null && Boolean.valueOf(body.get("directChat").toString());
            
            LocalDateTime interviewTime = body.get("interviewTime") != null ? parseTime(body.get("interviewTime").toString()) : null;
            String interviewLocation = body.get("interviewLocation") != null ? body.get("interviewLocation").toString() : null;
            Integer interviewType = body.get("interviewType") != null ? Integer.valueOf(body.get("interviewType").toString()) : 1;
            String contactPerson = body.get("contactPerson") != null ? body.get("contactPerson").toString() : null;
            String contactPhone = body.get("contactPhone") != null ? body.get("contactPhone").toString() : null;
            String notes = body.get("notes") != null ? body.get("notes").toString() : null;

            if (directChat && resumeId != null) {
                // 人才市场直接沟通模式
                Long interviewId = interviewService.createDirectChat(resumeId, user.getId(), interviewTime, notes);
                return ResponseEntity.ok(Map.of("message", "沟通通道已建立", "id", interviewId));
            } else {
                // 正常面试邀请流程
                interviewService.createInterview(applicationId, jobId, userId, user.getId(),
                        interviewTime, interviewLocation, interviewType, contactPerson, contactPhone, notes);
                return ResponseEntity.ok(Map.of("message", "面试邀请发送成功"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取我的面试邀请（求职者）
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyInterviews(Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            List<Map<String, Object>> interviews = interviewService.getMyInterviews(user.getId());
            return ResponseEntity.ok(interviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取企业发出的面试邀请
     */
    @GetMapping("/employer")
    public ResponseEntity<?> getEmployerInterviews(Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            List<Map<String, Object>> interviews = interviewService.getEmployerInterviews(user.getId());
            return ResponseEntity.ok(interviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新面试状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateInterviewStatus(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Integer status = Integer.valueOf(body.get("status").toString());
            boolean success = interviewService.updateInterviewStatus(id, status);
            if (success) {
                // 通知对方面试状态变更
                try {
                    Interview interview = interviewService.getById(id);
                    if (interview != null) {
                        Long notifyUserId = user.getId().equals(interview.getUserId())
                                ? interview.getEmployerId() : interview.getUserId();
                        String statusText = switch (status) {
                            case 1 -> "已确认";
                            case 2 -> "已取消";
                            case 3 -> "已完成";
                            default -> "待确认";
                        };
                        notificationService.send(
                                notifyUserId,
                                "面试状态更新",
                                String.format("面试（ID:%d）状态已更新为：%s", id, statusText),
                                "interview"
                        );
                    }
                } catch (Exception e) {
                    // 通知失败不影响主流程
                }
                return ResponseEntity.ok(Map.of("message", "状态更新成功"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "状态更新失败"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除面试记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInterview(@PathVariable Long id, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Interview interview = interviewService.getById(id);
            if (interview == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "面试记录不存在"));
            }
            // 只允许删除自己的面试记录
            if (!user.getId().equals(interview.getUserId()) && !user.getId().equals(interview.getEmployerId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "无权限删除此记录"));
            }

            interviewService.removeById(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取我的历史聊天列表
     */
    @GetMapping("/my-chats")
    public ResponseEntity<?> getMyChats(Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            List<Map<String, Object>> chats = interviewService.getMyChats(user.getId());
            return ResponseEntity.ok(chats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 根据 jobId 查询已有的直接沟通记录
     */
    @GetMapping("/chat")
    public ResponseEntity<?> getChatByJobId(@RequestParam Long jobId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            // 查询当前用户与该职位的直接沟通记录
            List<Map<String, Object>> chats = interviewService.getMyChats(user.getId());
            for (Map<String, Object> chat : chats) {
                Object chatJobId = chat.get("job_id");
                if (chatJobId != null && Long.valueOf(chatJobId.toString()).equals(jobId)) {
                    return ResponseEntity.ok(chat);
                }
            }
            return ResponseEntity.ok(Map.of("found", false));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 求职者发起与心仪职位的直接沟通
     */
    @PostMapping("/direct-chat")
    public ResponseEntity<?> createDirectChatFromJob(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Long jobId = body.get("jobId") != null ? Long.valueOf(body.get("jobId").toString()) : null;
            Long employerId = body.get("employerId") != null ? Long.valueOf(body.get("employerId").toString()) : null;
            String title = body.get("title") != null ? body.get("title").toString() : "在线沟通";

            if (jobId == null || employerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
            }

            // 创建直接沟通通道
            Long interviewId = interviewService.createDirectChatFromJob(jobId, employerId, user.getId(), title);
            return ResponseEntity.ok(Map.of("message", "沟通通道已建立", "id", interviewId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
