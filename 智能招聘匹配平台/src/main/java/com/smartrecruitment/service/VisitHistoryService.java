package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.VisitHistory;

import java.util.Map;

/**
 * 访问历史服务
 */
public interface VisitHistoryService extends IService<VisitHistory> {

    /**
     * 记录一次访问（异步执行）
     * isAnonymous: null 表示使用全局设置, true/false 表示单次覆盖
     */
    void recordVisit(Long visitorId, String username, String userType, String avatar,
                     Integer targetType, Long targetId, Long targetOwnerId, Boolean isAnonymous);

    /**
     * 查询谁访问了我（分页）
     * @param ownerId 被访问者ID
     * @param targetType 过滤类型: null=全部, 1=简历/主页, 2=职位
     */
    Map<String, Object> getMyVisitors(Long ownerId, Integer targetType, int page, int size);

    /**
     * 获取访问统计（今日/本周/总计）
     */
    Map<String, Object> getVisitStats(Long ownerId, Integer targetType);
}
