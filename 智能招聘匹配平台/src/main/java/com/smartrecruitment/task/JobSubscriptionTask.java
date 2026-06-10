package com.smartrecruitment.task;

import com.smartrecruitment.service.JobSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 职位订阅定时任务
 */
@Component
public class JobSubscriptionTask {
    
    private static final Logger log = LoggerFactory.getLogger(JobSubscriptionTask.class);
    
    @Autowired
    private JobSubscriptionService subscriptionService;
    
    /**
     * 每日早上9点执行订阅匹配
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailySubscriptionMatch() {
        log.info("========== 开始执行每日订阅匹配任务 ==========");
        try {
            int matchCount = subscriptionService.executeSubscriptionMatch();
            log.info("每日订阅匹配任务完成，共匹配 {} 个职位", matchCount);
        } catch (Exception e) {
            log.error("每日订阅匹配任务失败", e);
        }
        log.info("========== 每日订阅匹配任务结束 ==========");
    }
    
    /**
     * 每周一早上9点执行周报推送（备用，实际在executeSubscriptionMatch中已处理）
     */
    @Scheduled(cron = "0 0 9 ? * MON")
    public void weeklySubscriptionMatch() {
        log.info("========== 开始执行每周订阅匹配任务 ==========");
        // 实际逻辑已在executeSubscriptionMatch中统一处理
        log.info("每周订阅匹配任务已由daily任务统一处理");
        log.info("========== 每周订阅匹配任务结束 ==========");
    }
}
