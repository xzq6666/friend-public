package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartrecruitment.entity.CompanyInfo;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.CompanyInfoService;
import com.smartrecruitment.service.FavoriteService;
import com.smartrecruitment.service.JobApplicationService;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.JobSubscriptionService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.SensitiveWordService;
import com.smartrecruitment.service.OperationLogService;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.service.VisitHistoryService;
import com.smartrecruitment.utils.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ================================================
 * 职位控制器
 * ================================================
 *
 * URL 前缀：/job（需登录认证）
 *
 * 接口列表：
 *   POST   /job              → 发布新职位
 *   GET    /job/{id}         → 获取职位详情
 *   GET    /job              → 获取职位列表（分页）
 *   PUT    /job/{id}         → 更新职位信息
 *   DELETE /job/{id}         → 删除职位
 *   PUT    /job/{id}/status  → 上下架职位
 */
@RestController
@RequestMapping("/job")
@CrossOrigin
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyInfoService companyInfoService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private VisitHistoryService visitHistoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private JobSubscriptionService jobSubscriptionService;

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Job job, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            // 检查企业是否完成认证
            CompanyInfo companyInfo = companyInfoService.getByUserId(user.getId());
            if (companyInfo == null || companyInfo.getVerified() != 2) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "请先完成企业信息认证才能发布职位。请前往\"企业认证\"页面完善信息并提交审核。"
                ));
            }

            // 敏感词检测
            String textToCheck = (job.getTitle() != null ? job.getTitle() : "") + " "
                    + (job.getDescription() != null ? job.getDescription() : "") + " "
                    + (job.getRequirements() != null ? job.getRequirements() : "");
            List<String> found = sensitiveWordService.checkSensitiveWordsWithAi(textToCheck);
            if (!found.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "职位信息包含敏感词: " + String.join(", ", found),
                        "sensitiveWords", found
                ));
            }

            job.setEmployerId(user.getId());
            Job createdJob = jobService.createJob(job);

            // 触发实时订阅匹配
            try {
                jobSubscriptionService.matchAndNotifyForJob(createdJob);
            } catch (Exception e) {
                // 订阅匹配失败不影响职位发布
            }

            operationLogService.log(
                    user.getId(),
                    user.getUsername(),
                    "创建",
                    "职位",
                    String.format("发布了新职位 [%s]", createdJob.getTitle()),
                    IpUtils.getClientIp(),
                    1
            );
            return ResponseEntity.ok(createdJob);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "发布失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Long id, Authentication auth) {
        try {
            Job job = jobService.findById(id);
            if (job != null) {
                // 异步增加浏览量
                jobService.incrementViewCount(id);

                // 记录访问历史
                if (auth != null && !"anonymousUser".equals(auth.getName())) {
                    User visitor = userService.findByUsername(auth.getName());
                    if (visitor != null) {
                        visitHistoryService.recordVisit(visitor.getId(), visitor.getUsername(),
                                visitor.getUserType(), visitor.getAvatar(), 2, job.getId(), job.getEmployerId(), null);
                    }
                }

                // 附加企业信息
                Map<String, Object> result = new HashMap<>();
                result.put("id", job.getId());
                result.put("title", job.getTitle());
                result.put("description", job.getDescription());
                result.put("requirements", job.getRequirements());
                result.put("salaryMin", job.getSalaryMin());
                result.put("salaryMax", job.getSalaryMax());
                result.put("location", job.getLocation());
                result.put("experienceRequired", job.getExperienceRequired());
                result.put("educationRequired", job.getEducationRequired());
                result.put("workType", job.getWorkType());
                result.put("categoryId", job.getCategoryId());
                result.put("status", job.getStatus());
                result.put("createTime", job.getCreateTime());
                result.put("updateTime", job.getUpdateTime());
                result.put("employerId", job.getEmployerId());

                // 获取企业信息
                CompanyInfo companyInfo = companyInfoService.getByUserId(job.getEmployerId());
                if (companyInfo != null) {
                    // 隐藏敏感信息
                    companyInfo.setBusinessLicense(null);
                    result.put("companyInfo", companyInfo);
                }

                // 查询收藏状态（职位类型为1）
                if (auth != null) {
                    String username = auth.getName();
                    User user = userService.findByUsername(username);
                    if (user != null) {
                        boolean isFavorited = favoriteService.isFavorited(user.getId(), 1, job.getId());
                        result.put("isFavorited", isFavorited);
                    } else {
                        result.put("isFavorited", false);
                    }
                } else {
                    result.put("isFavorited", false);
                }

                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取职位浏览量
     */
    @GetMapping("/{id}/view-count")
    public ResponseEntity<?> getViewCount(@PathVariable Long id) {
        try {
            Long count = jobService.getViewCount(id);
            return ResponseEntity.ok(Map.of("viewCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long employerId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) Integer expMin,
            @RequestParam(required = false) Integer expMax,
            @RequestParam(required = false) String workType,
            Authentication auth) {
        try {
            // 获取当前登录用户ID（用于查询收藏状态）
            Long currentUserId = null;
            if (auth != null) {
                String username = auth.getName();
                User user = userService.findByUsername(username);
                if (user != null) {
                    currentUserId = user.getId();
                    // 如果是企业用户，只返回该企业的职位
                    if ("EMPLOYER".equals(user.getUserType())) {
                        employerId = user.getId();
                    }
                }
            }
            IPage<Job> jobPage = jobService.getJobPage(page, size, keyword, employerId, location, experience, education, salaryMin, salaryMax, expMin, expMax, workType);

            // 为每个职位附加企业信息和收藏状态
            List<Map<String, Object>> recordsWithCompany = new ArrayList<>();
            for (Job job : jobPage.getRecords()) {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("id", job.getId());
                jobMap.put("title", job.getTitle());
                jobMap.put("description", job.getDescription());
                jobMap.put("requirements", job.getRequirements());
                jobMap.put("salaryMin", job.getSalaryMin());
                jobMap.put("salaryMax", job.getSalaryMax());
                jobMap.put("location", job.getLocation());
                jobMap.put("experienceRequired", job.getExperienceRequired());
                jobMap.put("educationRequired", job.getEducationRequired());
                jobMap.put("workType", job.getWorkType());
                jobMap.put("categoryId", job.getCategoryId());
                jobMap.put("status", job.getStatus());
                jobMap.put("createTime", job.getCreateTime());
                jobMap.put("updateTime", job.getUpdateTime());
                jobMap.put("employerId", job.getEmployerId());
                
                // 获取企业信息
                CompanyInfo companyInfo = companyInfoService.getByUserId(job.getEmployerId());
                if (companyInfo != null) {
                    // 隐藏敏感信息
                    companyInfo.setBusinessLicense(null);
                    jobMap.put("companyInfo", companyInfo);
                }

                // 查询收藏状态（职位类型为1）
                if (currentUserId != null) {
                    boolean isFavorited = favoriteService.isFavorited(currentUserId, 1, job.getId());
                    jobMap.put("isFavorited", isFavorited);
                } else {
                    jobMap.put("isFavorited", false);
                }

                recordsWithCompany.add(jobMap);
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("records", recordsWithCompany);
            result.put("total", jobPage.getTotal());
            result.put("current", jobPage.getCurrent());
            result.put("size", jobPage.getSize());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody Job job, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            // 敏感词检测
            String textToCheck = (job.getTitle() != null ? job.getTitle() : "") + " "
                    + (job.getDescription() != null ? job.getDescription() : "") + " "
                    + (job.getRequirements() != null ? job.getRequirements() : "");
            if (!textToCheck.trim().isEmpty()) {
                List<String> found = sensitiveWordService.checkSensitiveWordsWithAi(textToCheck);
                if (!found.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "职位信息包含敏感词: " + String.join(", ", found),
                            "sensitiveWords", found
                    ));
                }
            }

            boolean success = jobService.updateJob(id, job);
            if (success) {
                if (currentUser != null) {
                    operationLogService.log(
                            currentUser.getId(),
                            currentUser.getUsername(),
                            "更新",
                            "职位",
                            String.format("更新了职位 [%s]", job.getTitle() != null ? job.getTitle() : "ID:" + id),
                            IpUtils.getClientIp(),
                            1
                    );
                }
                return ResponseEntity.ok(Map.of("message", "更新成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "更新失败"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            Job oldJob = jobService.findById(id);
            boolean success = jobService.deleteJob(id);
            if (success) {
                // 记录日志
                if (currentUser != null) {
                    operationLogService.log(
                            currentUser.getId(),
                            currentUser.getUsername(),
                            "删除",
                            "职位",
                            String.format("删除了职位 [%s]", oldJob != null ? oldJob.getTitle() : "ID:" + id),
                            IpUtils.getClientIp(),
                            1
                    );
                }
                return ResponseEntity.ok(Map.of("message", "删除成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateJobStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body, Authentication auth) {
        try {
            Integer status = body.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "状态值不能为空"));
            }
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            boolean success = jobService.updateJobStatus(id, status);
            if (success) {
                // 记录日志
                if (currentUser != null) {
                    operationLogService.log(
                            currentUser.getId(),
                            currentUser.getUsername(),
                            "更新",
                            "职位",
                            String.format("%s了职位 [ID:%d]", status == 1 ? "上架" : "下架", id),
                            IpUtils.getClientIp(),
                            1
                    );
                }
                // 职位下架时通知已投递的求职者
                if (status == 0) {
                    try {
                        Job job = jobService.getById(id);
                        if (job != null) {
                            List<JobApplication> applications = jobApplicationService.lambdaQuery()
                                    .eq(JobApplication::getJobId, id)
                                    .list();
                            List<Long> applicantIds = applications.stream()
                                    .map(JobApplication::getUserId)
                                    .distinct()
                                    .collect(java.util.stream.Collectors.toList());
                            notificationService.sendBatch(
                                    applicantIds,
                                    "职位已下架",
                                    String.format("您投递的职位 [%s] 已下架，如有疑问请联系企业方", job.getTitle()),
                                    "job_match"
                            );
                        }
                    } catch (Exception e) {
                        // 通知失败不影响主流程
                    }
                }
                return ResponseEntity.ok(Map.of("message", "状态更新成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "状态更新失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
