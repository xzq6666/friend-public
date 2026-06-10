package com.smartrecruitment.service;

import com.smartrecruitment.entity.Interview;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.mapper.InterviewMapper;
import com.smartrecruitment.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InterviewService 单元测试
 * 测试面试创建、状态流转、时间冲突检测等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @Mock
    private InterviewMapper interviewMapper;

    @Mock
    private JobApplicationService jobApplicationService;

    @Mock
    private ResumeService resumeService;

    @Mock
    private NotificationService notificationService;

    private Interview testInterview;
    private JobApplication testApplication;

    @BeforeEach
    void setUp() {
        testInterview = new Interview();
        testInterview.setId(1L);
        testInterview.setApplicationId(1L);
        testInterview.setJobId(1L);
        testInterview.setUserId(200L);
        testInterview.setEmployerId(100L);
        testInterview.setInterviewTime(LocalDateTime.now().plusDays(3));
        testInterview.setInterviewLocation("公司总部3楼会议室");
        testInterview.setInterviewType(1); // 现场面试
        testInterview.setStatus(0); // 待确认

        testApplication = new JobApplication();
        testApplication.setId(1L);
        testApplication.setUserId(200L);
        testApplication.setJobId(1L);
        testApplication.setStatus(1); // 已查看
    }

    // ======================== 创建面试测试 ========================

    @Test
    void testCreateInterview_success() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(3);

        // 模拟投递记录存在且状态合法
        when(jobApplicationService.getById(1L)).thenReturn(testApplication);
        when(jobApplicationService.updateApplicationStatus(anyLong(), anyInt(), any())).thenReturn(true);
        // 模拟无时间冲突
        when(interviewMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(interviewMapper.insert(any(Interview.class))).thenReturn(1);

        Interview result = interviewService.createInterview(
                1L, 1L, 200L, 100L,
                futureTime, "公司总部", 1, "HR", "13800138000", "请准时到达"
        );

        assertNotNull(result, "创建面试应返回面试记录");
        assertEquals(0, result.getStatus(), "新面试状态应为 0（待确认）");
        verify(notificationService).send(eq(200L), anyString(), anyString(), eq("interview"));
    }

    @Test
    void testCreateInterview_pastTime_shouldThrowException() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(2);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            interviewService.createInterview(
                    1L, 1L, 200L, 100L,
                    pastTime, "公司总部", 1, "HR", "13800138000", null
            );
        });
        assertEquals("面试时间不能早于当前时间", ex.getMessage());
    }

    @Test
    void testCreateInterview_applicationNotExists_shouldThrowException() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(3);

        // 模拟无时间冲突
        when(interviewMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(jobApplicationService.getById(999L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            interviewService.createInterview(
                    999L, 1L, 200L, 100L,
                    futureTime, "公司总部", 1, "HR", "13800138000", null
            );
        });
        assertEquals("投递记录不存在", ex.getMessage());
    }

    @Test
    void testCreateInterview_invalidApplicationStatus_shouldThrowException() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(3);
        testApplication.setStatus(0); // 待处理（不能安排面试）

        // 模拟无时间冲突
        when(interviewMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(jobApplicationService.getById(1L)).thenReturn(testApplication);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            interviewService.createInterview(
                    1L, 1L, 200L, 100L,
                    futureTime, "公司总部", 1, "HR", "13800138000", null
            );
        });
        assertEquals("该投递当前状态不能安排面试", ex.getMessage());
    }

    // ======================== 面试状态流转测试 ========================

    @Test
    void testUpdateStatus_pendingToConfirmed_success() {
        when(interviewMapper.selectById(1L)).thenReturn(testInterview);
        when(interviewMapper.updateById(any(Interview.class))).thenReturn(1);

        boolean result = interviewService.updateInterviewStatus(1L, 1);

        assertTrue(result, "待确认->已确认 应成功");
    }

    @Test
    void testUpdateStatus_pendingToCancelled_success() {
        when(interviewMapper.selectById(1L)).thenReturn(testInterview);
        when(interviewMapper.updateById(any(Interview.class))).thenReturn(1);

        boolean result = interviewService.updateInterviewStatus(1L, 2);

        assertTrue(result, "待确认->已取消 应成功");
    }

    @Test
    void testUpdateStatus_confirmedToCompleted_success() {
        testInterview.setStatus(1); // 已确认
        when(interviewMapper.selectById(1L)).thenReturn(testInterview);
        when(interviewMapper.updateById(any(Interview.class))).thenReturn(1);

        boolean result = interviewService.updateInterviewStatus(1L, 3);

        assertTrue(result, "已确认->已完成 应成功");
    }

    @Test
    void testUpdateStatus_cancelledToAny_shouldFail() {
        testInterview.setStatus(2); // 已取消（终态）
        when(interviewMapper.selectById(1L)).thenReturn(testInterview);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            interviewService.updateInterviewStatus(1L, 1);
        });
        assertTrue(ex.getMessage().contains("面试状态流转不合法"));
    }

    @Test
    void testUpdateStatus_completedToAny_shouldFail() {
        testInterview.setStatus(3); // 已完成（终态）
        when(interviewMapper.selectById(1L)).thenReturn(testInterview);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            interviewService.updateInterviewStatus(1L, 2);
        });
        assertTrue(ex.getMessage().contains("面试状态流转不合法"));
    }

    @Test
    void testUpdateStatus_nonExistent_shouldReturnFalse() {
        when(interviewMapper.selectById(999L)).thenReturn(null);

        boolean result = interviewService.updateInterviewStatus(999L, 1);

        assertFalse(result, "不存在的面试应返回 false");
    }
}
