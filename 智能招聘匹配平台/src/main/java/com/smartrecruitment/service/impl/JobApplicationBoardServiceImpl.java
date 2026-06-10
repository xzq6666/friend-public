package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruitment.entity.Favorite;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.service.FavoriteService;
import com.smartrecruitment.service.JobApplicationBoardService;
import com.smartrecruitment.service.JobApplicationService;
import com.smartrecruitment.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 求职进度看板服务实现
 */
@Service
public class JobApplicationBoardServiceImpl implements JobApplicationBoardService {
    
    @Autowired
    private JobApplicationService applicationService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private JobService jobService;
    
    @Autowired
    private com.smartrecruitment.service.UserService userService;
    
    @Override
    public Map<String, Object> getBoardData(Long userId) {
        Map<String, Object> board = new LinkedHashMap<>();
        
        // 1. 已收藏（从favorites表获取）
        List<Map<String, Object>> favorites = getFavoriteJobs(userId);
        board.put("favorited", favorites);
        
        // 2. 已投递（status=0，最近3天内）
        List<Map<String, Object>> applied = getApplicationsByStatus(userId, 0, true);
        board.put("applied", applied);
        
        // 3. 初筛中（status=0，超过3天）
        List<Map<String, Object>> screening = getApplicationsByStatus(userId, 0, false);
        board.put("screening", screening);
        
        // 4. 面试中（status=3）
        List<Map<String, Object>> interviewing = getApplicationsByStatus(userId, 3, null);
        board.put("interviewing", interviewing);
        
        // 5. 已录用（status=4）
        List<Map<String, Object>> hired = getApplicationsByStatus(userId, 4, null);
        board.put("hired", hired);
        
        // 6. 已拒绝（status=2）
        List<Map<String, Object>> rejected = getApplicationsByStatus(userId, 2, null);
        board.put("rejected", rejected);
        
        return board;
    }
    
    /**
     * 获取收藏的职位
     */
    private List<Map<String, Object>> getFavoriteJobs(Long userId) {
        // target_type: 1-职位 2-简历
        List<Favorite> favorites = favoriteService.lambdaQuery()
            .eq(Favorite::getUserId, userId)
            .eq(Favorite::getTargetType, 1) // 1表示职位
            .orderByDesc(Favorite::getCreateTime)
            .list();
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite fav : favorites) {
            Job job = jobService.getById(fav.getTargetId());
            if (job != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "fav_" + fav.getId());
                item.put("type", "favorite");
                item.put("jobId", job.getId());
                item.put("jobTitle", job.getTitle());
                
                // 获取公司名称
                String companyName = getCompanyName(job.getEmployerId());
                item.put("companyName", companyName);
                
                item.put("salaryMin", job.getSalaryMin());
                item.put("salaryMax", job.getSalaryMax());
                item.put("location", job.getLocation());
                item.put("favoriteTime", fav.getCreateTime());
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * 根据状态获取申请列表
     * @param within3Days true=3天内（已投递），false=超过3天（初筛中），null=不限
     */
    private List<Map<String, Object>> getApplicationsByStatus(Long userId, Integer status, Boolean within3Days) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobApplication::getUserId, userId)
               .eq(JobApplication::getStatus, status);
        
        if (within3Days != null) {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            if (within3Days) {
                wrapper.ge(JobApplication::getCreateTime, threeDaysAgo);
            } else {
                wrapper.lt(JobApplication::getCreateTime, threeDaysAgo);
            }
        }
        
        wrapper.orderByDesc(JobApplication::getCreateTime);
        
        List<JobApplication> applications = applicationService.list(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (JobApplication app : applications) {
            Job job = jobService.getById(app.getJobId());
            if (job != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "app_" + app.getId());
                item.put("type", "application");
                item.put("applicationId", app.getId());
                item.put("jobId", job.getId());
                item.put("jobTitle", job.getTitle());
                
                // 获取公司名称
                String companyName = getCompanyName(job.getEmployerId());
                item.put("companyName", companyName);
                
                item.put("salaryMin", job.getSalaryMin());
                item.put("salaryMax", job.getSalaryMax());
                item.put("location", job.getLocation());
                item.put("status", app.getStatus());
                item.put("applyTime", app.getCreateTime());
                item.put("updateTime", app.getUpdateTime());
                
                // 计算停留天数
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    app.getCreateTime(), LocalDateTime.now()
                );
                item.put("daysInStage", days);
                
                result.add(item);
            }
        }
        
        return result;
    }
    
    @Override
    public boolean updateApplicationStatus(Long applicationId, Integer newStatus, Long userId) {
        JobApplication application = applicationService.getById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        
        if (!application.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此申请");
        }
        
        application.setStatus(newStatus);
        application.setUpdateTime(LocalDateTime.now());
        
        return applicationService.updateById(application);
    }
    
    @Override
    public Map<String, Object> getStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 各阶段数量
        long favoritedCount = favoriteService.lambdaQuery()
            .eq(Favorite::getUserId, userId)
            .eq(Favorite::getTargetType, 1) // 1表示职位
            .count();
        
        long appliedCount = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .eq(JobApplication::getStatus, 0)
            .ge(JobApplication::getCreateTime, LocalDateTime.now().minusDays(3))
            .count();
        
        long screeningCount = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .eq(JobApplication::getStatus, 0)
            .lt(JobApplication::getCreateTime, LocalDateTime.now().minusDays(3))
            .count();
        
        long interviewingCount = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .eq(JobApplication::getStatus, 3)
            .count();
        
        long hiredCount = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .eq(JobApplication::getStatus, 4)
            .count();
        
        long rejectedCount = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .eq(JobApplication::getStatus, 2)
            .count();
        
        stats.put("favorited", favoritedCount);
        stats.put("applied", appliedCount);
        stats.put("screening", screeningCount);
        stats.put("interviewing", interviewingCount);
        stats.put("hired", hiredCount);
        stats.put("rejected", rejectedCount);
        
        // 总投递数
        long totalApplications = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .count();
        stats.put("totalApplications", totalApplications);
        
        // 成功率
        if (totalApplications > 0) {
            double successRate = (double) hiredCount / totalApplications * 100;
            stats.put("successRate", String.format("%.1f%%", successRate));
        } else {
            stats.put("successRate", "0%");
        }
        
        // 平均响应时间（天）
        stats.put("avgResponseTime", calculateAvgResponseTime(userId));
        
        return stats;
    }
    
    /**
     * 计算平均响应时间
     */
    private String calculateAvgResponseTime(Long userId) {
        List<JobApplication> applications = applicationService.lambdaQuery()
            .eq(JobApplication::getUserId, userId)
            .in(JobApplication::getStatus, Arrays.asList(1, 2, 3, 4))
            .list();
        
        if (applications.isEmpty()) {
            return "-";
        }
        
        double totalDays = 0;
        int count = 0;
        
        for (JobApplication app : applications) {
            if (app.getUpdateTime() != null && app.getCreateTime() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    app.getCreateTime(), app.getUpdateTime()
                );
                totalDays += days;
                count++;
            }
        }
        
        if (count > 0) {
            double avg = totalDays / count;
            return String.format("%.1f天", avg);
        }
        
        return "-";
    }
    
    /**
     * 获取公司名称
     */
    private String getCompanyName(Long employerId) {
        if (employerId == null) {
            return "未知公司";
        }
        com.smartrecruitment.entity.User employer = userService.getById(employerId);
        return employer != null ? employer.getUsername() : "未知公司";
    }
}
