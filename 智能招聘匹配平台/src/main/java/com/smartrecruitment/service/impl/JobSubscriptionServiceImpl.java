package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobSubscription;
import com.smartrecruitment.entity.JobSubscriptionPushLog;
import com.smartrecruitment.mapper.JobSubscriptionMapper;
import com.smartrecruitment.mapper.JobSubscriptionPushLogMapper;
import com.smartrecruitment.service.AIService;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.JobSubscriptionService;
import com.smartrecruitment.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 职位订阅服务实现
 */
@Service
public class JobSubscriptionServiceImpl extends ServiceImpl<JobSubscriptionMapper, JobSubscription> 
        implements JobSubscriptionService {
    
    private static final Logger log = LoggerFactory.getLogger(JobSubscriptionServiceImpl.class);
    
    @Autowired
    private JobService jobService;

    @Autowired
    private AIService aiService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JobSubscriptionPushLogMapper pushLogMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public JobSubscription createSubscription(JobSubscription subscription) {
        // 验证关键词
        if (subscription.getKeywords() == null || subscription.getKeywords().isEmpty()) {
            throw new IllegalArgumentException("订阅关键词不能为空");
        }
        
        // 设置默认值
        if (subscription.getPushStrategy() == null) {
            subscription.setPushStrategy("daily");
        }
        if (subscription.getIsActive() == null) {
            subscription.setIsActive(1);
        }
        if (subscription.getMatchCount() == null) {
            subscription.setMatchCount(0);
        }
        
        save(subscription);
        log.info("创建职位订阅: userId={}, name={}", subscription.getUserId(), subscription.getName());
        return subscription;
    }
    
    @Override
    @Transactional
    public boolean updateSubscription(JobSubscription subscription) {
        JobSubscription existing = getById(subscription.getId());
        if (existing == null) {
            throw new IllegalArgumentException("订阅不存在");
        }
        
        // 权限检查
        if (!existing.getUserId().equals(subscription.getUserId())) {
            throw new IllegalArgumentException("无权限修改此订阅");
        }
        
        return updateById(subscription);
    }
    
    @Override
    @Transactional
    public boolean deleteSubscription(Long id, Long userId) {
        JobSubscription subscription = getById(id);
        if (subscription == null) {
            throw new IllegalArgumentException("订阅不存在");
        }
        
        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除此订阅");
        }
        
        return removeById(id);
    }
    
    @Override
    public List<JobSubscription> getUserSubscriptions(Long userId) {
        return lambdaQuery()
            .eq(JobSubscription::getUserId, userId)
            .orderByDesc(JobSubscription::getCreateTime)
            .list();
    }
    
    @Override
    @Transactional
    public boolean toggleSubscription(Long id, Long userId, Integer isActive) {
        JobSubscription subscription = getById(id);
        if (subscription == null) {
            throw new IllegalArgumentException("订阅不存在");
        }
        
        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此订阅");
        }
        
        subscription.setIsActive(isActive);
        return updateById(subscription);
    }
    
    @Override
    @Transactional
    public int executeSubscriptionMatch() {
        log.info("开始执行职位订阅匹配任务");
        int totalMatches = 0;
        
        // 1. 处理实时推送（暂不处理，由发布职位时触发）
        
        // 2. 处理每日推送
        totalMatches += processSubscriptionsByStrategy("daily");
        
        // 3. 处理每周推送
        totalMatches += processSubscriptionsByStrategy("weekly");
        
        log.info("职位订阅匹配任务完成，共匹配 {} 个职位", totalMatches);
        return totalMatches;
    }
    
    /**
     * 处理指定策略的订阅
     */
    private int processSubscriptionsByStrategy(String strategy) {
        List<JobSubscription> subscriptions = baseMapper.findSubscriptionsByStrategy(strategy);
        log.info("找到 {} 个需要{}推送的订阅", subscriptions.size(), strategy);
        
        int matchCount = 0;
        for (JobSubscription sub : subscriptions) {
            try {
                matchCount += matchAndNotify(sub);
            } catch (Exception e) {
                log.error("订阅匹配失败: subscriptionId={}", sub.getId(), e);
            }
        }
        
        return matchCount;
    }
    
    /**
     * 匹配职位并发送通知（AI智能匹配 + 去重）
     */
    private int matchAndNotify(JobSubscription subscription) {
        // 1. 预筛选：用宽松条件获取候选职位
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getStatus, 1);
        if (subscription.getCategoryId() != null) {
            wrapper.eq(Job::getCategoryId, subscription.getCategoryId());
        }
        wrapper.last("LIMIT 100");

        List<Job> candidateJobs = jobService.list(wrapper);

        if (candidateJobs.isEmpty()) {
            log.info("订阅 {} 无候选职位", subscription.getId());
            return 0;
        }

        // 2. AI智能匹配
        List<Map<String, Object>> aiMatches;
        try {
            aiMatches = aiService.matchJobsForSubscription(subscription, candidateJobs);
        } catch (Exception e) {
            log.error("AI匹配失败，降级为关键词匹配: subscriptionId={}", subscription.getId(), e);
            // 降级：使用原有关键词匹配
            return fallbackMatchAndNotify(subscription);
        }

        if (aiMatches.isEmpty()) {
            log.info("订阅 {} AI未匹配到职位", subscription.getId());
            return 0;
        }

        List<Long> matchedJobIds = aiMatches.stream()
                .map(m -> (Long) m.get("job_id"))
                .filter(Objects::nonNull)
                .toList();

        // 3. 过滤已推送过的职位
        List<Long> pushedJobIds = pushLogMapper.selectList(
                new LambdaQueryWrapper<JobSubscriptionPushLog>()
                        .eq(JobSubscriptionPushLog::getSubscriptionId, subscription.getId())
                        .in(JobSubscriptionPushLog::getJobId, matchedJobIds)
        ).stream().map(JobSubscriptionPushLog::getJobId).toList();

        // 记录AI匹配理由
        for (Map<String, Object> m : aiMatches) {
            Long jobId = (Long) m.get("job_id");
            String reason = (String) m.get("reason");
            if (jobId != null && reason != null) {
                log.debug("AI匹配: jobId={}, reason={}", jobId, reason);
            }
        }

        List<Job> newJobs = candidateJobs.stream()
                .filter(j -> matchedJobIds.contains(j.getId()) && !pushedJobIds.contains(j.getId()))
                .toList();

        if (newJobs.isEmpty()) {
            log.info("订阅 {} 匹配到的职位已全部推送过", subscription.getId());
            return 0;
        }

        // 4. 发送通知
        sendSubscriptionNotification(subscription, newJobs);

        // 5. 记录推送日志
        for (Job job : newJobs) {
            JobSubscriptionPushLog logEntry = new JobSubscriptionPushLog();
            logEntry.setSubscriptionId(subscription.getId());
            logEntry.setJobId(job.getId());
            logEntry.setPushTime(LocalDateTime.now());
            pushLogMapper.insert(logEntry);
        }

        // 6. 更新订阅统计
        subscription.setLastPushTime(LocalDateTime.now());
        subscription.setMatchCount(subscription.getMatchCount() + newJobs.size());
        updateById(subscription);

        log.info("订阅 {} AI匹配到 {} 个新职位", subscription.getId(), newJobs.size());
        return newJobs.size();
    }

    /**
     * 降级匹配：当AI不可用时使用关键词匹配
     */
    private int fallbackMatchAndNotify(JobSubscription subscription) {
        LambdaQueryWrapper<Job> wrapper = buildJobQueryWrapper(subscription);
        List<Job> matchedJobs = jobService.list(wrapper.last("LIMIT 50"));
        if (matchedJobs.isEmpty()) return 0;

        List<Long> pushedJobIds = pushLogMapper.selectList(
                new LambdaQueryWrapper<JobSubscriptionPushLog>()
                        .eq(JobSubscriptionPushLog::getSubscriptionId, subscription.getId())
                        .in(JobSubscriptionPushLog::getJobId, matchedJobs.stream().map(Job::getId).toList())
        ).stream().map(JobSubscriptionPushLog::getJobId).toList();

        List<Job> newJobs = matchedJobs.stream()
                .filter(j -> !pushedJobIds.contains(j.getId()))
                .toList();

        if (newJobs.isEmpty()) return 0;

        sendSubscriptionNotification(subscription, newJobs);

        for (Job job : newJobs) {
            JobSubscriptionPushLog logEntry = new JobSubscriptionPushLog();
            logEntry.setSubscriptionId(subscription.getId());
            logEntry.setJobId(job.getId());
            logEntry.setPushTime(LocalDateTime.now());
            pushLogMapper.insert(logEntry);
        }

        subscription.setLastPushTime(LocalDateTime.now());
        subscription.setMatchCount(subscription.getMatchCount() + newJobs.size());
        updateById(subscription);

        return newJobs.size();
    }

    /**
     * 构建职位查询条件（复用于定时匹配和实时匹配）
     */
    private LambdaQueryWrapper<Job> buildJobQueryWrapper(JobSubscription subscription) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getStatus, 1);

        if (subscription.getKeywords() != null && !subscription.getKeywords().isEmpty()) {
            try {
                List<String> keywords = objectMapper.readValue(subscription.getKeywords(), List.class);
                if (!keywords.isEmpty()) {
                    // 关键词之间是 OR 关系：匹配任意一个即可
                    wrapper.and(w -> {
                        for (int i = 0; i < keywords.size(); i++) {
                            String keyword = keywords.get(i);
                            if (i > 0) w.or();
                            w.and(inner -> inner.like(Job::getTitle, keyword)
                                    .or()
                                    .like(Job::getDescription, keyword)
                                    .or()
                                    .like(Job::getRequirements, keyword));
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("解析订阅关键词失败: {}", subscription.getKeywords());
            }
        }

        if (subscription.getCategoryId() != null) {
            wrapper.eq(Job::getCategoryId, subscription.getCategoryId());
        }
        if (subscription.getSalaryMax() != null) {
            wrapper.le(Job::getSalaryMin, subscription.getSalaryMax());
        }
        if (subscription.getSalaryMin() != null) {
            wrapper.ge(Job::getSalaryMax, subscription.getSalaryMin());
        }
        if (subscription.getLocation() != null && !subscription.getLocation().isEmpty()) {
            wrapper.like(Job::getLocation, subscription.getLocation());
        }
        if (subscription.getExperienceRequired() != null) {
            wrapper.eq(Job::getExperienceRequired, subscription.getExperienceRequired());
        }
        if (subscription.getEducationRequired() != null) {
            wrapper.eq(Job::getEducationRequired, subscription.getEducationRequired());
        }

        return wrapper;
    }
    
    /**
     * 发送订阅通知
     */
    private void sendSubscriptionNotification(JobSubscription subscription, List<Job> jobs) {
        StringBuilder title = new StringBuilder("🎯 订阅提醒：发现 ");
        title.append(jobs.size()).append(" 个新职位");
        
        StringBuilder content = new StringBuilder("根据您的订阅「");
        content.append(subscription.getName()).append("」，发现以下匹配职位：\n\n");
        
        for (int i = 0; i < Math.min(5, jobs.size()); i++) {
            Job job = jobs.get(i);
            content.append(i + 1).append(". ").append(job.getTitle());
            if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
                content.append(" | 薪资：")
                       .append(job.getSalaryMin().intValue())
                       .append("-")
                       .append(job.getSalaryMax().intValue())
                       .append("K");
            }
            if (job.getLocation() != null) {
                content.append(" | 地点：").append(job.getLocation());
            }
            content.append("\n");
        }
        
        if (jobs.size() > 5) {
            content.append("\n...还有 ").append(jobs.size() - 5).append(" 个职位，请登录查看详情");
        }

        // 附加第一个匹配职位的ID，供前端跳转
        if (!jobs.isEmpty()) {
            content.append("\njobId:").append(jobs.get(0).getId());
        }

        notificationService.send(subscription.getUserId(), title.toString(), content.toString(), "job_match");
        log.info("发送订阅通知: userId={}, jobId count={}", subscription.getUserId(), jobs.size());
    }
    
    @Override
    @Transactional
    public Map<String, Object> manualTriggerMatch(Long subscriptionId, Long userId) {
        JobSubscription subscription = getById(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("订阅不存在");
        }
        
        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此订阅");
        }
        
        int matchCount = matchAndNotify(subscription);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("matchCount", matchCount);
        result.put("message", matchCount > 0 ? "找到 " + matchCount + " 个匹配职位" : "暂无新匹配职位");

        return result;
    }

    @Override
    @Async
    public void matchAndNotifyForJob(Job job) {
        // 查询所有启用的实时推送订阅
        List<JobSubscription> realtimeSubs = lambdaQuery()
                .eq(JobSubscription::getIsActive, 1)
                .eq(JobSubscription::getPushStrategy, "realtime")
                .list();

        if (realtimeSubs.isEmpty()) {
            return;
        }

        for (JobSubscription sub : realtimeSubs) {
            try {
                if (matchesJob(sub, job)) {
                    // 检查是否已推送
                    Long existCount = pushLogMapper.selectCount(
                            new LambdaQueryWrapper<JobSubscriptionPushLog>()
                                    .eq(JobSubscriptionPushLog::getSubscriptionId, sub.getId())
                                    .eq(JobSubscriptionPushLog::getJobId, job.getId()));
                    if (existCount > 0) continue;

                    // 发送通知
                    sendSubscriptionNotification(sub, List.of(job));

                    // 记录推送日志
                    JobSubscriptionPushLog logEntry = new JobSubscriptionPushLog();
                    logEntry.setSubscriptionId(sub.getId());
                    logEntry.setJobId(job.getId());
                    logEntry.setPushTime(LocalDateTime.now());
                    pushLogMapper.insert(logEntry);

                    // 更新统计
                    sub.setLastPushTime(LocalDateTime.now());
                    sub.setMatchCount(sub.getMatchCount() + 1);
                    updateById(sub);

                    log.info("实时推送: subscription={}, job={}", sub.getId(), job.getId());
                }
            } catch (Exception e) {
                log.error("实时推送失败: subscriptionId={}", sub.getId(), e);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getPushHistory(Long subscriptionId, Long userId) {
        // 权限检查
        JobSubscription subscription = getById(subscriptionId);
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订阅不存在或无权限");
        }

        // 查询推送记录
        List<JobSubscriptionPushLog> logs = pushLogMapper.selectList(
                new LambdaQueryWrapper<JobSubscriptionPushLog>()
                        .eq(JobSubscriptionPushLog::getSubscriptionId, subscriptionId)
                        .orderByDesc(JobSubscriptionPushLog::getPushTime)
                        .last("LIMIT 100"));

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询关联的职位
        List<Long> jobIds = logs.stream().map(JobSubscriptionPushLog::getJobId).toList();
        Map<Long, Job> jobMap = jobService.listByIds(jobIds).stream()
                .collect(java.util.stream.Collectors.toMap(Job::getId, j -> j));

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (JobSubscriptionPushLog logEntry : logs) {
            Job job = jobMap.get(logEntry.getJobId());
            Map<String, Object> item = new HashMap<>();
            item.put("pushTime", logEntry.getPushTime());
            item.put("jobId", logEntry.getJobId());
            if (job != null) {
                item.put("title", job.getTitle());
                item.put("salaryMin", job.getSalaryMin());
                item.put("salaryMax", job.getSalaryMax());
                item.put("location", job.getLocation());
                item.put("experienceRequired", job.getExperienceRequired());
                item.put("educationRequired", job.getEducationRequired());
                item.put("status", job.getStatus());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 检查职位是否匹配订阅条件
     */
    private boolean matchesJob(JobSubscription sub, Job job) {
        // 关键词匹配
        if (sub.getKeywords() != null && !sub.getKeywords().isEmpty()) {
            try {
                List<String> keywords = objectMapper.readValue(sub.getKeywords(), List.class);
                String text = ((job.getTitle() != null ? job.getTitle() : "") + " " +
                        (job.getDescription() != null ? job.getDescription() : "") + " " +
                        (job.getRequirements() != null ? job.getRequirements() : "")).toLowerCase();
                boolean keywordMatch = keywords.stream().anyMatch(kw -> text.contains(kw.toLowerCase()));
                if (!keywordMatch) return false;
            } catch (Exception e) {
                // 关键词解析失败，跳过关键词匹配
            }
        }

        // 行业分类
        if (sub.getCategoryId() != null && !sub.getCategoryId().equals(job.getCategoryId())) {
            return false;
        }

        // 薪资范围
        if (sub.getSalaryMax() != null && job.getSalaryMin() != null && job.getSalaryMin().compareTo(sub.getSalaryMax()) > 0) {
            return false;
        }
        if (sub.getSalaryMin() != null && job.getSalaryMax() != null && job.getSalaryMax().compareTo(sub.getSalaryMin()) < 0) {
            return false;
        }

        // 地点
        if (sub.getLocation() != null && !sub.getLocation().isEmpty() && job.getLocation() != null) {
            if (!job.getLocation().contains(sub.getLocation())) return false;
        }

        // 经验要求
        if (sub.getExperienceRequired() != null && !sub.getExperienceRequired().isEmpty()
                && job.getExperienceRequired() != null) {
            if (!job.getExperienceRequired().equals(sub.getExperienceRequired())) return false;
        }

        // 学历要求
        if (sub.getEducationRequired() != null && !sub.getEducationRequired().isEmpty()
                && job.getEducationRequired() != null) {
            if (!job.getEducationRequired().equals(sub.getEducationRequired())) return false;
        }

        return true;
    }
}
