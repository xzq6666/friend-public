package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.service.NotificationService;import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.JobApplicationService;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.OperationLogService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.utils.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 投递记录控制器
 */
@RestController
@RequestMapping("/application")
@CrossOrigin
public class JobApplicationController {

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private JobService jobService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 投递职位
     */
    @PostMapping
    public ResponseEntity<?> applyJob(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            Long jobId = Long.valueOf(body.get("jobId").toString());
            String coverLetter = body.get("coverLetter") != null ? body.get("coverLetter").toString() : null;
            
            // 支持指定简历ID，如果没有则使用默认简历
            Long resumeId;
            if (body.get("resumeId") != null) {
                resumeId = Long.valueOf(body.get("resumeId").toString());
                // 验证简历是否属于当前用户
                Resume selectedResume = resumeService.getById(resumeId);
                if (selectedResume == null || !selectedResume.getUserId().equals(user.getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "简历不存在或无权限"));
                }
            } else {
                // 获取用户默认简历
                Resume resume = resumeService.getDefaultResumeByUserId(user.getId());
                if (resume == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历再投递"));
                }
                resumeId = resume.getId();
            }

            JobApplication application = jobApplicationService.applyJob(user.getId(), resumeId, jobId, coverLetter);

            // 创建通知给企业用户
            try {
                Job job = jobService.getById(jobId);
                if (job != null && job.getEmployerId() != null) {
                    notificationService.send(
                            job.getEmployerId(),
                            "收到新的简历投递",
                            String.format("求职者 %s 投递了您的职位 [%s]", user.getUsername(), job.getTitle()),
                            "job_match"
                    );
                }
            } catch (Exception e) {
                System.err.println("创建投递通知失败: " + e.getMessage());
            }

            // 记录日志
            operationLogService.log(
                    user.getId(),
                    user.getUsername(),
                    "投递",
                    "职位",
                    String.format("投递了职位 [ID:%d]", jobId),
                    IpUtils.getClientIp(),
                    1
            );
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "投递失败：" + e.getMessage()));
        }
    }

    /**
     * 获取我的投递记录
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyApplications(Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            List<Map<String, Object>> applications = jobApplicationService.getUserApplications(user.getId());
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取职位的投递记录（企业查看，仅限职位发布者）
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getJobApplications(@PathVariable Long jobId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            // 验证是否为该职位的发布者或管理员
            Job job = jobService.getById(jobId);
            if (job == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));
            }
            if (!"ADMIN".equals(user.getUserType()) && !user.getId().equals(job.getEmployerId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权查看该职位的投递记录"));
            }
            List<Map<String, Object>> applications = jobApplicationService.getJobApplications(jobId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 分页获取候选人列表（企业查看投递记录）
     */
    @GetMapping("/page")
    public ResponseEntity<?> getCandidatesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            // 只有企业和管理员可以查看候选人列表
            if (!"EMPLOYER".equals(user.getUserType()) && !"ADMIN".equals(user.getUserType())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权限访问"));
            }

            // 非管理员只能查看自己发布的职位的候选人
            Long employerId = "ADMIN".equals(user.getUserType()) ? null : user.getId();

            IPage<Map<String, Object>> candidatePage = jobApplicationService.getCandidatesPage(page, size, employerId, jobId, status, keyword);
            return ResponseEntity.ok(candidatePage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新投递状态（企业操作）
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            Object statusObj = body.get("status");

            Integer status = statusObj != null ? Integer.valueOf(statusObj.toString()) : null;
            String rejectReason = body.get("rejectReason") != null ? body.get("rejectReason").toString() : null;

            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "状态值不能为空"));
            }

            // 验证当前用户是否为该投递记录对应职位的发布者
            JobApplication app = jobApplicationService.getById(id);
            if (app == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "投递记录不存在"));
            }
            Job job = jobService.getById(app.getJobId());
            if (job == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "关联职位不存在"));
            }
            // 只有发布该职位的企业才能修改投递状态（管理员不可操作）
            if (!currentUser.getId().equals(job.getEmployerId())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作此投递记录"));
            }

            boolean success = jobApplicationService.updateApplicationStatus(id, status, rejectReason);
            
            if (success) {
                // 记录日志
                String statusDesc = switch (status) {
                    case 1 -> "已查看";
                    case 2 -> "邀请面试";
                    case 3 -> "已录用";
                    case 4 -> "已拒绝";
                    default -> "状态变更";
                };
                operationLogService.log(
                        currentUser.getId(),
                        currentUser.getUsername(),
                        "更新",
                        "投递",
                        String.format("将投递记录 [ID:%d] 状态更新为 [%s]", id, statusDesc),
                        IpUtils.getClientIp(),
                        1
                );

                // 通知候选人状态变更
                try {
                    String notifyContent = status == 4
                        ? String.format("您投递的职位 [%s] 未通过：%s", job.getTitle(), rejectReason != null ? rejectReason : "暂无说明")
                        : String.format("您投递的职位 [%s] 状态已更新为：%s", job.getTitle(), statusDesc);
                    notificationService.send(app.getUserId(), "投递状态更新", notifyContent, "job_match");
                } catch (Exception e) {
                    // 通知失败不影响主流程
                }

                return ResponseEntity.ok(Map.of("message", "状态更新成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "状态更新失败，记录可能不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "更新失败: " + e.getMessage()));
        }
    }

    /**
     * 检查是否已投递
     */
    @GetMapping("/check/{jobId}")
    public ResponseEntity<?> checkApplied(@PathVariable Long jobId, Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            boolean hasApplied = jobApplicationService.hasApplied(user.getId(), jobId);
            return ResponseEntity.ok(Map.of("hasApplied", hasApplied));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
