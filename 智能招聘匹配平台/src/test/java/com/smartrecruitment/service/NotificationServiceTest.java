package com.smartrecruitment.service;

import com.smartrecruitment.entity.Notification;
import com.smartrecruitment.entity.NotificationPreference;
import com.smartrecruitment.mapper.NotificationMapper;
import com.smartrecruitment.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

/**
 * NotificationService 单元测试
 * 测试通知发送、批量发送、删除、偏好检查等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(100L);
        testNotification.setTitle("面试邀请");
        testNotification.setContent("您收到一个面试邀请");
        testNotification.setType("interview");
        testNotification.setIsRead(0);
    }

    // ======================== 发送通知测试 ========================

    @Test
    void testSend_success() {
        // 无偏好设置，默认开启
        when(notificationPreferenceService.getByUserId(100L)).thenReturn(null);
        Mockito.doReturn(1).when(notificationMapper).insert((Notification) any());

        notificationService.send(100L, "面试邀请", "您收到一个面试邀请", "interview");

        verify(notificationMapper).insert((Notification) any());
    }

    @Test
    void testSend_nullUserId_shouldNotSend() {
        notificationService.send(null, "标题", "内容", "system");

        verify(notificationMapper, never()).insert((Notification) any());
    }

    @Test
    void testSend_nullType_shouldNotSend() {
        notificationService.send(100L, "标题", "内容", null);

        verify(notificationMapper, never()).insert((Notification) any());
    }

    @Test
    void testSend_notificationDisabled_shouldNotSend() {
        // 模拟用户关闭了面试通知
        NotificationPreference prefs = new NotificationPreference();
        prefs.setUserId(100L);
        prefs.setInterviewNotify(0); // 关闭面试通知
        when(notificationPreferenceService.getByUserId(100L)).thenReturn(prefs);

        notificationService.send(100L, "面试邀请", "内容", "interview");

        verify(notificationMapper, never()).insert((Notification) any());
    }

    @Test
    void testSend_systemNotificationDisabled_shouldNotSend() {
        NotificationPreference prefs = new NotificationPreference();
        prefs.setUserId(100L);
        prefs.setSystemNotify(0); // 关闭系统通知
        when(notificationPreferenceService.getByUserId(100L)).thenReturn(prefs);

        notificationService.send(100L, "系统通知", "内容", "system");

        verify(notificationMapper, never()).insert((Notification) any());
    }

    // ======================== 批量发送测试 ========================

    @Test
    void testSendBatch_success() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        when(notificationPreferenceService.getByUserId(100L)).thenReturn(null);
        Mockito.doReturn(1).when(notificationMapper).insert((Notification) any());

        notificationService.sendBatch(userIds, "系统公告", "平台升级通知", "system");

        verify(notificationMapper, times(3)).insert((Notification) any());
    }

    @Test
    void testSendBatch_emptyList_shouldNotSend() {
        notificationService.sendBatch(List.of(), "标题", "内容", "system");

        verify(notificationMapper, never()).insert((Notification) any());
    }

    @Test
    void testSendBatch_nullList_shouldNotSend() {
        notificationService.sendBatch(null, "标题", "内容", "system");

        verify(notificationMapper, never()).insert((Notification) any());
    }

    // ======================== 删除通知测试 ========================

    @Test
    void testDeleteNotification_success() {
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);
        when(notificationMapper.deleteById(1L)).thenReturn(1);

        boolean result = notificationService.deleteNotification(1L, 100L);

        assertTrue(result, "删除自己的通知应成功");
    }

    @Test
    void testDeleteNotification_notOwner_shouldFail() {
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);

        boolean result = notificationService.deleteNotification(1L, 999L); // 非所有者

        assertFalse(result, "删除他人通知应失败");
    }

    @Test
    void testDeleteNotification_notExists_shouldFail() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        boolean result = notificationService.deleteNotification(999L, 100L);

        assertFalse(result, "不存在的通知应返回 false");
    }
}
