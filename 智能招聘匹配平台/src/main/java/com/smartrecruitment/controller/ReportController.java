package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.*;
import com.smartrecruitment.mapper.ForumCommentMapper;
import com.smartrecruitment.mapper.ForumPostMapper;
import com.smartrecruitment.mapper.JobMapper;
import com.smartrecruitment.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 举报控制器
 */
@RestController
@RequestMapping("/report")
@CrossOrigin
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ForumPostService forumPostService;

    @Autowired
    private ForumCommentService forumCommentService;

    @Autowired
    private ForumPostMapper forumPostMapper;

    @Autowired
    private ForumCommentMapper forumCommentMapper;

    @Autowired
    private JobMapper jobMapper;

    /**
     * 提交举报
     */
    @PostMapping
    public ResponseEntity<?> submitReport(@RequestBody Report report, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        if (report.getReportedType() == null || report.getReportedId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "举报类型和被举报对象ID不能为空"));
        }
        if (report.getReason() == null || report.getReason().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "举报原因不能为空"));
        }

        // 防重复举报：同用户 + 同类型 + 同对象 且状态为待处理/处理中
        Long duplicateCount = reportService.lambdaQuery()
                .eq(Report::getReporterId, currentUser.getId())
                .eq(Report::getReportedType, report.getReportedType())
                .eq(Report::getReportedId, report.getReportedId())
                .in(Report::getStatus, 0, 1)
                .count();
        if (duplicateCount > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "您已举报过该内容，请等待管理员处理"));
        }

        report.setReporterId(currentUser.getId());
        Report saved = reportService.submitReport(report);

        // 通知所有管理员
        try {
            List<User> admins = userService.lambdaQuery()
                    .eq(User::getUserType, "ADMIN")
                    .list();
            List<Long> adminIds = admins.stream().map(User::getId).collect(Collectors.toList());
            if (!adminIds.isEmpty()) {
                int type = report.getReportedType() != null ? report.getReportedType() : 0;
                String typeLabel = switch (type) {
                    case 1 -> "帖子";
                    case 2 -> "评论";
                    case 3 -> "用户";
                    case 4 -> "职位";
                    default -> "内容";
                };
                notificationService.sendBatch(
                        adminIds,
                        "收到新举报",
                        String.format("用户 %s 举报了一条%s，原因：%s\nreportId:%d",
                                currentUser.getUsername(), typeLabel, report.getReason(), saved.getId()),
                        "system"
                );
            }
        } catch (Exception e) {
            // 通知失败不影响举报主流程
        }

        return ResponseEntity.ok(Map.of(
                "message", "举报已提交，我们会尽快处理",
                "reportId", saved.getId()
        ));
    }

    /**
     * 获取我的举报列表（求职者/企业）
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer reportedType,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        IPage<Report> reportPage = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, currentUser.getId());
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        if (reportedType != null) {
            wrapper.eq(Report::getReportedType, reportedType);
        }
        wrapper.orderByDesc(Report::getCreateTime);

        reportPage = reportService.page(reportPage, wrapper);

        return ResponseEntity.ok(reportPage);
    }

    /**
     * 获取所有举报列表（管理员） — 附加举报人用户名和被举报内容摘要
     */
    @GetMapping("/list")
    public ResponseEntity<?> getReportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer reportedType,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        IPage<Report> reportPage = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        if (reportedType != null) {
            wrapper.eq(Report::getReportedType, reportedType);
        }

        wrapper.orderByDesc(Report::getCreateTime);

        reportPage = reportService.page(reportPage, wrapper);

        // 附加举报人用户名和被举报内容摘要
        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (Report report : reportPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", report.getId());
            item.put("reporterId", report.getReporterId());
            item.put("reportedType", report.getReportedType());
            item.put("reportedId", report.getReportedId());
            item.put("reason", report.getReason());
            item.put("description", report.getDescription());
            item.put("evidenceUrls", report.getEvidenceUrls());
            item.put("status", report.getStatus());
            item.put("handlerId", report.getHandlerId());
            item.put("handleResult", report.getHandleResult());
            item.put("handleTime", report.getHandleTime());
            item.put("createTime", report.getCreateTime());

            // 举报人用户名
            User reporter = userService.getById(report.getReporterId());
            item.put("reporterUsername", reporter != null ? reporter.getUsername() : "未知用户");

            // 被举报内容摘要
            item.put("reportedContent", getReportedContentSummary(report.getReportedType(), report.getReportedId()));

            enrichedRecords.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", reportPage.getTotal());
        result.put("current", reportPage.getCurrent());
        result.put("size", reportPage.getSize());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取举报统计数据（管理员）
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getReportStats(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }
        return ResponseEntity.ok(reportService.getStats());
    }

    /**
     * 处理举报（管理员） — 支持处罚操作 + 通知举报人
     */
    @PutMapping("/{id}/handle")
    public ResponseEntity<?> handleReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }

            User currentUser = userService.findByUsername(auth.getName());
            if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
            }

            // 安全类型转换：Jackson 可能将数字反序列化为 Long 或 Integer
            Integer status = null;
            Object statusObj = body.get("status");
            if (statusObj instanceof Number) {
                status = ((Number) statusObj).intValue();
            }
            String handleResult = body.get("handleResult") != null ? body.get("handleResult").toString() : null;
            boolean deleteContent = Boolean.TRUE.equals(body.get("deleteContent"));
            boolean banUser = Boolean.TRUE.equals(body.get("banUser"));

            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "处理状态不能为空"));
            }

            Report report = reportService.getById(id);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            boolean success = reportService.handleReport(id, status, handleResult, currentUser.getId());

            if (success) {
                // 先确定被举报内容的作者（用于后续通知）
                // 注意：使用 getById 而非 getPostDetail，避免已删除内容查不到作者
                Long reportedUserId = null;
                try {
                    if (report.getReportedType() == 1) {
                        ForumPost post = forumPostMapper.selectById(report.getReportedId());
                        if (post != null) reportedUserId = post.getUserId();
                    } else if (report.getReportedType() == 2) {
                        ForumComment comment = forumCommentMapper.selectById(report.getReportedId());
                        if (comment != null) reportedUserId = comment.getUserId();
                    } else if (report.getReportedType() == 3) {
                        reportedUserId = report.getReportedId();
                    } else if (report.getReportedType() == 4) {
                        Job job = jobMapper.selectById(report.getReportedId());
                        if (job != null) reportedUserId = job.getEmployerId();
                    }
                } catch (Exception e) {
                    // ignore
                }

                // 处罚操作：删除被举报内容（直接通过 mapper 操作，确保生效）
                boolean contentDeleted = false;
                if (deleteContent && status == 2) {
                    try {
                        if (report.getReportedType() == 1) {
                            ForumPost post = forumPostMapper.selectById(report.getReportedId());
                            if (post != null && post.getStatus() != 0) {
                                post.setStatus(0);
                                contentDeleted = forumPostMapper.updateById(post) > 0;
                            }
                        } else if (report.getReportedType() == 2) {
                            ForumComment comment = forumCommentMapper.selectById(report.getReportedId());
                            if (comment != null && comment.getStatus() != 0) {
                                comment.setStatus(0);
                                contentDeleted = forumCommentMapper.updateById(comment) > 0;
                            }
                        } else if (report.getReportedType() == 4) {
                            Job job = jobMapper.selectById(report.getReportedId());
                            if (job != null && job.getStatus() != 0) {
                                job.setStatus(0);
                                contentDeleted = jobMapper.updateById(job) > 0;
                            }
                        }
                    } catch (Exception e) {
                        // 处罚失败不影响主流程
                    }
                }

                // 处罚操作：禁言用户
                boolean userBanned = false;
                if (banUser && status == 2 && reportedUserId != null) {
                    try {
                        User targetUser = userService.getById(reportedUserId);
                        if (targetUser != null) {
                            targetUser.setStatus(0);
                            userService.updateById(targetUser);
                            userBanned = true;
                        }
                    } catch (Exception e) {
                        // 处罚失败不影响主流程
                    }
                }

                // 通知举报人处理结果
                try {
                    String statusText = switch (status) {
                        case 2 -> "已处理";
                        case 3 -> "已驳回";
                        default -> "处理中";
                    };
                    notificationService.send(
                            report.getReporterId(),
                            "举报处理结果",
                            String.format("您提交的举报（ID:%d）%s。%s",
                                    id, statusText,
                                    handleResult != null && !handleResult.isEmpty() ? "处理说明：" + handleResult : ""),
                            "system"
                    );
                } catch (Exception e) {
                    // 通知失败不影响主流程
                }

                // 通知被举报用户处罚结果
                if (reportedUserId != null && status == 2) {
                    try {
                        StringBuilder notifyContent = new StringBuilder();
                        String contentType = switch (report.getReportedType()) {
                            case 1 -> "帖子";
                            case 2 -> "评论";
                            case 3 -> "账号";
                            case 4 -> "职位";
                            default -> "内容";
                        };
                        notifyContent.append("您的").append(contentType).append("因违规被管理员处理。");
                        if (contentDeleted) {
                            if (report.getReportedType() == 4) {
                                notifyContent.append("违规职位已被下架。");
                            } else {
                                notifyContent.append("违规内容已被删除。");
                            }
                        }
                        if (userBanned) {
                            notifyContent.append("您的账号已被禁用，如有疑问请联系管理员。");
                        }
                        if (handleResult != null && !handleResult.isEmpty()) {
                            notifyContent.append("处理说明：").append(handleResult);
                        }
                        notificationService.send(
                                reportedUserId,
                                "违规处理通知",
                                notifyContent.toString(),
                                "system"
                        );
                    } catch (Exception e) {
                        // 通知失败不影响主流程
                    }
                }

                return ResponseEntity.ok(Map.of("message", "处理成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "处理失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 删除举报（管理员）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }
        boolean success = reportService.removeById(id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "删除失败，举报不存在"));
    }

    /**
     * 获取举报详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReportDetail(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Report report = reportService.getById(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        if (!report.getReporterId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权查看此举报"));
        }

        return ResponseEntity.ok(report);
    }

    /**
     * 获取被举报内容的跳转路径（管理员）
     * type=1 帖子 → /forum/{jobId}
     * type=2 评论 → /forum/{jobId}
     * type=3 用户 → /company/{userId} 或 /r/resume/{userId}
     * type=4 职位 → /browse-jobs
     */
    @GetMapping("/jump-url")
    public ResponseEntity<?> getJumpUrl(
            @RequestParam Integer reportedType,
            @RequestParam Long reportedId,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足"));
        }

        String url = null;
        try {
            if (reportedType == 1) {
                ForumPost post = forumPostMapper.selectById(reportedId);
                if (post != null) url = "/forum/" + post.getJobId() + "?highlightPost=" + reportedId;
            } else if (reportedType == 2) {
                ForumComment comment = forumCommentMapper.selectById(reportedId);
                if (comment != null) {
                    ForumPost post = forumPostMapper.selectById(comment.getPostId());
                    if (post != null) url = "/forum/" + post.getJobId() + "?highlightComment=" + reportedId;
                }
            } else if (reportedType == 3) {
                User user = userService.getById(reportedId);
                if (user != null) {
                    if ("EMPLOYER".equals(user.getUserType())) {
                        url = "/company/" + reportedId;
                    } else {
                        url = "/r/resume/" + reportedId;
                    }
                }
            } else if (reportedType == 4) {
                Job job = jobMapper.selectById(reportedId);
                if (job != null) url = "/browse-jobs?highlightJob=" + reportedId;
            }
        } catch (Exception e) {
            // ignore
        }

        if (url == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "内容已删除或不存在"));
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 获取被举报内容摘要
     */
    private String getReportedContentSummary(Integer reportedType, Long reportedId) {
        try {
            if (reportedType == 1) {
                ForumPost post = forumPostService.getPostDetail(reportedId);
                return post != null ? "帖子: " + post.getTitle() : "帖子已删除";
            } else if (reportedType == 2) {
                ForumComment comment = forumCommentService.getById(reportedId);
                if (comment != null) {
                    String content = comment.getContent();
                    return "评论: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content);
                }
                return "评论已删除";
            } else if (reportedType == 3) {
                User user = userService.getById(reportedId);
                return user != null ? "用户: " + user.getUsername() : "用户不存在";
            } else if (reportedType == 4) {
                Job job = jobMapper.selectById(reportedId);
                return job != null ? "职位: " + job.getTitle() : "职位已下线";
            }
        } catch (Exception e) {
            // ignore
        }
        return "未知内容";
    }
}
