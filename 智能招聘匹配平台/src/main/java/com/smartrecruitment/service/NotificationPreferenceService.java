package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.NotificationPreference;

/**
 * 通知偏好服务接口
 */
public interface NotificationPreferenceService extends IService<NotificationPreference> {

    /**
     * 获取用户通知偏好（不存在则返回 null）
     */
    NotificationPreference getByUserId(Long userId);
}
