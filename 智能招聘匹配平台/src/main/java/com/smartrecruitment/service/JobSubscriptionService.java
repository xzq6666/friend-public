package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobSubscription;

import java.util.List;
import java.util.Map;

/**
 * 职位订阅服务接口
 */
public interface JobSubscriptionService extends IService<JobSubscription> {
    
    /**
     * 创建订阅
     */
    JobSubscription createSubscription(JobSubscription subscription);
    
    /**
     * 更新订阅
     */
    boolean updateSubscription(JobSubscription subscription);
    
    /**
     * 删除订阅
     */
    boolean deleteSubscription(Long id, Long userId);
    
    /**
     * 查询用户的订阅列表
     */
    List<JobSubscription> getUserSubscriptions(Long userId);
    
    /**
     * 激活/停用订阅
     */
    boolean toggleSubscription(Long id, Long userId, Integer isActive);
    
    /**
     * 执行订阅匹配（定时任务调用）
     * @return 匹配的职位数量
     */
    int executeSubscriptionMatch();
    
    /**
     * 手动触发订阅匹配
     */
    Map<String, Object> manualTriggerMatch(Long subscriptionId, Long userId);

    /**
     * 新职位发布时触发实时订阅匹配（异步）
     */
    void matchAndNotifyForJob(Job job);

    /**
     * 获取订阅的推送历史（含职位详情）
     */
    List<Map<String, Object>> getPushHistory(Long subscriptionId, Long userId);
}
