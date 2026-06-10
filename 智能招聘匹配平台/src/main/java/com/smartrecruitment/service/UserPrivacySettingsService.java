package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.UserPrivacySettings;

/**
 * 用户隐私设置服务
 */
public interface UserPrivacySettingsService extends IService<UserPrivacySettings> {

    /**
     * 获取用户隐私设置（不存在则创建默认设置）
     */
    UserPrivacySettings getOrCreateByUserId(Long userId);

    /**
     * 更新用户隐私设置
     */
    void updateSettings(Long userId, Boolean defaultAnonymous, Boolean showVisitHistory);
}
