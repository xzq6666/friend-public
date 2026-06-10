package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Notification;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 统一发送通知（检查用户偏好，偏好关闭则跳过）
     *
     * @param userId  接收者ID
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型：job_match / interview / system / chat / announcement
     */
    void send(Long userId, String title, String content, String type);

    /**
     * 批量发送通知（检查用户偏好）
     */
    void sendBatch(List<Long> userIds, String title, String content, String type);

    /**
     * 删除单条通知（校验所有权）
     */
    boolean deleteNotification(Long notificationId, Long userId);
}
