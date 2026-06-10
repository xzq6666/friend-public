package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Notification;
import com.smartrecruitment.entity.NotificationPreference;
import com.smartrecruitment.mapper.NotificationMapper;
import com.smartrecruitment.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知服务实现
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Autowired
    private com.smartrecruitment.service.NotificationPreferenceService notificationPreferenceService;

    @Override
    @Async
    public void send(Long userId, String title, String content, String type) {
        if (userId == null || type == null) return;

        // 检查用户通知偏好
        if (!isNotifyEnabled(userId, type)) return;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        save(notification);
    }

    @Override
    @Async
    public void sendBatch(List<Long> userIds, String title, String content, String type) {
        if (userIds == null || userIds.isEmpty() || type == null) return;

        for (Long userId : userIds) {
            if (isNotifyEnabled(userId, type)) {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType(type);
                notification.setIsRead(0);
                save(notification);
            }
        }
    }

    @Override
    public boolean deleteNotification(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null) return false;
        if (!notification.getUserId().equals(userId)) return false;
        return removeById(notificationId);
    }

    /**
     * 检查用户是否开启了某类型通知
     */
    private boolean isNotifyEnabled(Long userId, String type) {
        NotificationPreference prefs = notificationPreferenceService.getByUserId(userId);
        if (prefs == null) return true; // 无偏好设置时默认开启
        return switch (type) {
            case "job_match" -> prefs.getJobMatchNotify() == 1;
            case "interview" -> prefs.getInterviewNotify() == 1;
            case "chat" -> prefs.getChatNotify() == 1;
            case "system", "announcement" -> prefs.getSystemNotify() == 1;
            default -> true;
        };
    }
}
