package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.UserPrivacySettings;
import com.smartrecruitment.mapper.UserPrivacySettingsMapper;
import com.smartrecruitment.service.UserPrivacySettingsService;
import org.springframework.stereotype.Service;

/**
 * 用户隐私设置服务实现
 */
@Service
public class UserPrivacySettingsServiceImpl extends ServiceImpl<UserPrivacySettingsMapper, UserPrivacySettings>
        implements UserPrivacySettingsService {

    @Override
    public UserPrivacySettings getOrCreateByUserId(Long userId) {
        UserPrivacySettings settings = lambdaQuery()
                .eq(UserPrivacySettings::getUserId, userId)
                .one();
        if (settings == null) {
            settings = new UserPrivacySettings();
            settings.setUserId(userId);
            settings.setDefaultAnonymous(0);
            settings.setShowVisitHistory(1);
            save(settings);
        }
        return settings;
    }

    @Override
    public void updateSettings(Long userId, Boolean defaultAnonymous, Boolean showVisitHistory) {
        UserPrivacySettings settings = getOrCreateByUserId(userId);
        if (defaultAnonymous != null) {
            settings.setDefaultAnonymous(defaultAnonymous ? 1 : 0);
        }
        if (showVisitHistory != null) {
            settings.setShowVisitHistory(showVisitHistory ? 1 : 0);
        }
        updateById(settings);
    }
}
