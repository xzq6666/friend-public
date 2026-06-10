package com.smartrecruitment.controller;

import com.smartrecruitment.mapper.MatchRecordMapper;
import com.smartrecruitment.service.CompanyInfoService;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.ReportService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ================================================
 * 管理后台控制器 - 数据统计
 * ================================================
 *
 * URL 前缀：/admin（需登录认证）
 *
 * 接口列表：
 *   GET /admin/stats → 获取系统统计数据（用户数/简历数/职位数/匹配数）
 *
 * 如需扩展：
 *   - 添加 GET /admin/stats/daily 每日统计数据
 *   - 添加 GET /admin/stats/monthly 月度统计报表
 *   - 添加 GET /admin/logs 系统操作日志
 *   - 添加 GET /admin/config 系统配置管理
 *   - 添加 POST /admin/cache/clear 清除缓存
 *   - 添加 GET /admin/health 系统健康检查
 *   - 添加 GET /admin/today-stats 今日新增数据
 *   - 添加用户活跃度统计（日活/月活）
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private CompanyInfoService companyInfoService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private MatchRecordMapper matchRecordMapper;

    /**
     * 公开统计接口（登录页面使用）
     */
    @GetMapping("/stats/public")
    public ResponseEntity<?> getPublicStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", jobService.count());
        stats.put("totalResumes", resumeService.count());
        stats.put("totalMatches", matchRecordMapper.selectCount(null));
        stats.put("totalUsers", userService.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats")
    // // @PreAuthorize("hasRole('ADMIN')")  // 开发环境暂时移除  // 开发环境暂时移除权限限制
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userService.count());
        stats.put("resumeCount", resumeService.count());
        stats.put("jobCount", jobService.count());
        stats.put("matchCount", matchRecordMapper.selectCount(null));

        // 今日新增数据
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        stats.put("todayUsers", userService.countByDateRange(today, tomorrow));
        stats.put("todayResumes", resumeService.countByDateRange(today, tomorrow));
        stats.put("todayJobs", jobService.countByDateRange(today, tomorrow));
        stats.put("todayMatches", matchRecordMapper.countByDateRange(today, tomorrow));

        // 待审核数量
        long pendingCompanyVerifies = companyInfoService.lambdaQuery()
                .eq(com.smartrecruitment.entity.CompanyInfo::getVerified, 1).count();
        stats.put("pendingCompanyVerifies", pendingCompanyVerifies);

        long pendingReports = reportService.lambdaQuery()
                .eq(com.smartrecruitment.entity.Report::getStatus, 0).count();
        stats.put("pendingReports", pendingReports);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/detailed")
    // @PreAuthorize("hasRole('ADMIN')")  // 开发环境暂时移除
    public ResponseEntity<?> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();

        // 基础统计
        stats.put("totalUsers", userService.count());
        stats.put("totalResumes", resumeService.count());
        stats.put("totalJobs", jobService.count());
        stats.put("totalMatches", matchRecordMapper.selectCount(null));

        // 用户分布统计
        Map<String, Long> userTypeStats = new HashMap<>();
        userTypeStats.put("admin", userService.countByType("ADMIN"));
        userTypeStats.put("employer", userService.countByType("EMPLOYER"));
        userTypeStats.put("employee", userService.countByType("EMPLOYEE"));
        stats.put("userTypeDistribution", userTypeStats);

        // 今日新增数据
        LocalDateTime today = LocalDate.now().atStartOfDay();
        stats.put("todayUsers", userService.countByCreateTime(today));
        stats.put("todayResumes", resumeService.countByCreateTime(today));
        stats.put("todayJobs", jobService.countByCreateTime(today));
        stats.put("todayMatches", matchRecordMapper.countByCreateTime(today));

        // 活跃用户统计（最近7天）
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        stats.put("activeUsers7d", userService.countActiveUsers(weekAgo));

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/trends")
    // @PreAuthorize("hasRole('ADMIN')")  // 开发环境暂时移除
    public ResponseEntity<?> getTrends(@RequestParam(defaultValue = "7") int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Map<String, Object>> trends = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            LocalDateTime dayStart = date.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd")));
            dayData.put("users", userService.countByDateRange(dayStart, dayEnd));
            dayData.put("resumes", resumeService.countByDateRange(dayStart, dayEnd));
            dayData.put("jobs", jobService.countByDateRange(dayStart, dayEnd));
            dayData.put("matches", matchRecordMapper.countByDateRange(dayStart, dayEnd));

            trends.add(dayData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("trends", trends);
        result.put("period", days + "天");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/popular-jobs")
    // @PreAuthorize("hasRole('ADMIN')")  // 开发环境暂时移除
    public ResponseEntity<?> getPopularJobs() {
        List<Map<String, Object>> popularJobs = jobService.getPopularJobs(10);
        return ResponseEntity.ok(popularJobs);
    }

    @GetMapping("/stats/top-skills")
    // @PreAuthorize("hasRole('ADMIN')")  // 开发环境暂时移除
    public ResponseEntity<?> getTopSkills() {
        List<Map<String, Object>> topSkills = resumeService.getTopSkills(10);
        return ResponseEntity.ok(topSkills);
    }

    /**
     * 企业端统计数据（企业用户可访问）
     */
    @GetMapping("/stats/employer")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<?> getEmployerStats(
            Authentication auth) {
        String username = auth.getName();
        com.smartrecruitment.entity.User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Long employerId = user.getId();
        Map<String, Object> stats = new HashMap<>();

        // 我的职位数
        long myJobs = jobService.lambdaQuery().eq(com.smartrecruitment.entity.Job::getEmployerId, employerId).count();
        stats.put("myJobs", myJobs);

        // 收到投递数
        List<com.smartrecruitment.entity.Job> myJobList = jobService.lambdaQuery()
                .eq(com.smartrecruitment.entity.Job::getEmployerId, employerId).list();
        List<Long> jobIds = myJobList.stream().map(com.smartrecruitment.entity.Job::getId).collect(java.util.stream.Collectors.toList());

        long totalApplications = 0;
        long pendingApplications = 0;
        long interviewCount = 0;
        if (!jobIds.isEmpty()) {
            // 批量查询投递数量
            totalApplications = applicationService.lambdaQuery()
                    .in(com.smartrecruitment.entity.JobApplication::getJobId, jobIds)
                    .count();
            pendingApplications = applicationService.lambdaQuery()
                    .in(com.smartrecruitment.entity.JobApplication::getJobId, jobIds)
                    .eq(com.smartrecruitment.entity.JobApplication::getStatus, 0)
                    .count();
        }

        stats.put("totalApplications", totalApplications);
        stats.put("pendingApplications", pendingApplications);

        // 面试安排数（包括待确认和已确认的面试）
        if (!jobIds.isEmpty()) {
            interviewCount = interviewService.lambdaQuery()
                    .in(com.smartrecruitment.entity.Interview::getJobId, jobIds)
                    .in(com.smartrecruitment.entity.Interview::getStatus, 0, 1)
                    .count();
        }
        stats.put("interviewCount", interviewCount);

        // 今日新增投递
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        long todayApplications = 0;
        if (!jobIds.isEmpty()) {
            todayApplications = applicationService.lambdaQuery()
                    .in(com.smartrecruitment.entity.JobApplication::getJobId, jobIds)
                    .ge(com.smartrecruitment.entity.JobApplication::getCreateTime, today)
                    .lt(com.smartrecruitment.entity.JobApplication::getCreateTime, tomorrow)
                    .count();
        }
        stats.put("todayApplications", todayApplications);

        // 招聘漏斗数据
        long screenedApplications = 0;
        long hiredCount = 0;
        if (!jobIds.isEmpty()) {
            // 已筛选简历：状态>=1（已查看/邀请面试/已录用），排除已拒绝
            screenedApplications = applicationService.lambdaQuery()
                    .in(com.smartrecruitment.entity.JobApplication::getJobId, jobIds)
                    .ge(com.smartrecruitment.entity.JobApplication::getStatus, 1)
                    .ne(com.smartrecruitment.entity.JobApplication::getStatus, 4)
                    .count();
            // 已录用人数
            hiredCount = applicationService.lambdaQuery()
                    .in(com.smartrecruitment.entity.JobApplication::getJobId, jobIds)
                    .eq(com.smartrecruitment.entity.JobApplication::getStatus, 3)
                    .count();
        }
        stats.put("screenedApplications", screenedApplications);
        stats.put("hiredCount", hiredCount);

        return ResponseEntity.ok(stats);
    }

    /**
     * 企业端投递趋势（企业用户可访问）
     */
    @GetMapping("/stats/employer/trends")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<?> getEmployerTrends(@RequestParam(defaultValue = "7") int days, Authentication auth) {
        String username = auth.getName();
        com.smartrecruitment.entity.User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        Long employerId = user.getId();
        List<com.smartrecruitment.entity.Job> myJobs = jobService.lambdaQuery()
                .eq(com.smartrecruitment.entity.Job::getEmployerId, employerId).list();
        List<Long> jobIds = myJobs.stream().map(com.smartrecruitment.entity.Job::getId).collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> trends = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            LocalDateTime dayStart = date.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd")));

            long appCount = 0;
            if (!jobIds.isEmpty()) {
                appCount = applicationService.lambdaQuery()
                        .in(com.smartrecruitment.entity.JobApplication::getJobId, jobIds)
                        .ge(com.smartrecruitment.entity.JobApplication::getCreateTime, dayStart)
                        .lt(com.smartrecruitment.entity.JobApplication::getCreateTime, dayEnd)
                        .count();
            }
            dayData.put("applications", appCount);
            trends.add(dayData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("trends", trends);
        result.put("period", days + "天");
        return ResponseEntity.ok(result);
    }

    @Autowired
    private com.smartrecruitment.service.JobApplicationService applicationService;

    @Autowired
    private com.smartrecruitment.service.InterviewService interviewService;
}
