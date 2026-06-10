package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.NotificationPreference;
import com.smartrecruitment.mapper.NotificationPreferenceMapper;
import com.smartrecruitment.service.NotificationPreferenceService;
import org.springframework.stereotype.Service;

/**
 * 通知偏好服务实现
 */
@Service
public class NotificationPreferenceServiceImpl extends ServiceImpl<NotificationPreferenceMapper, NotificationPreference>
        implements NotificationPreferenceService {

    @Override
    public NotificationPreference getByUserId(Long userId) {
        return lambdaQuery().eq(NotificationPreference::getUserId, userId).one();
    }
}
