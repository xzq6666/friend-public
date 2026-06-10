package com.smartrecruitment.service;

import java.util.List;
import java.util.Map;

/**
 * 求职进度看板服务接口
 */
public interface JobApplicationBoardService {
    
    /**
     * 获取用户的看板数据
     * @param userId 用户ID
     * @return 包含各列数据的Map
     */
    Map<String, Object> getBoardData(Long userId);
    
    /**
     * 更新申请状态（拖拽卡片时调用）
     * @param applicationId 申请ID
     * @param newStatus 新状态
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean updateApplicationStatus(Long applicationId, Integer newStatus, Long userId);
    
    /**
     * 获取统计数据
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getStatistics(Long userId);
}
