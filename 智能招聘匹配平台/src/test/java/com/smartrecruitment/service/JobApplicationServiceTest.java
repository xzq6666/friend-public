package com.smartrecruitment.service;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.mapper.JobApplicationMapper;
import com.smartrecruitment.mapper.JobMapper;
import com.smartrecruitment.service.impl.JobApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JobApplicationService 单元测试
 * 测试职位投递、状态流转、重复投递校验等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    @Mock
    private JobApplicationMapper jobApplicationMapper;

    @Mock
    private JobMapper jobMapper;

    private Job testJob;
    private JobApplication testApplication;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId(1L);
        testJob.setEmployerId(100L);
        testJob.setTitle("Java工程师");
        testJob.setStatus(1);

        testApplication = new JobApplication();
        testApplication.setId(1L);
        testApplication.setUserId(200L);
        testApplication.setResumeId(1L);
        testApplication.setJobId(1L);
        testApplication.setStatus(0); // 待处理
    }

    // ======================== 投递简历测试 ========================

    @Test
    void testApplyJob_success() {
        // 模拟未投递过
        when(jobApplicationMapper.selectCount(any())).thenReturn(0L);
        // 模拟职位存在且在线
        when(jobMapper.selectById(1L)).thenReturn(testJob);
        when(jobApplicationMapper.insert(any(JobApplication.class))).thenReturn(1);

        JobApplication result = jobApplicationService.applyJob(200L, 1L, 1L, "我对该职位很感兴趣");

        assertNotNull(result, "投递应返回申请记录");
        assertEquals(200L, result.getUserId());
        assertEquals(1L, result.getJobId());
        assertEquals(0, result.getStatus(), "新投递状态应为 0（待处理）");
    }

    @Test
    void testApplyJob_alreadyApplied_shouldThrowException() {
        // 模拟已投递过
        when(jobApplicationMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.applyJob(200L, 1L, 1L, "再次投递");
        });
        assertEquals("您已经投递过该职位", ex.getMessage());
    }

    @Test
    void testApplyJob_jobNotExists_shouldThrowException() {
        when(jobApplicationMapper.selectCount(any())).thenReturn(0L);
        when(jobMapper.selectById(999L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.applyJob(200L, 1L, 999L, "投递");
        });
        assertEquals("职位不存在", ex.getMessage());
    }

    @Test
    void testApplyJob_jobOffline_shouldThrowException() {
        testJob.setStatus(0); // 已下线
        when(jobApplicationMapper.selectCount(any())).thenReturn(0L);
        when(jobMapper.selectById(1L)).thenReturn(testJob);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.applyJob(200L, 1L, 1L, "投递");
        });
        assertEquals("该职位已下线，暂不接受投递", ex.getMessage());
    }

    @Test
    void testApplyJob_selfJob_shouldThrowException() {
        // 模拟投递自己发布的职位
        when(jobApplicationMapper.selectCount(any())).thenReturn(0L);
        when(jobMapper.selectById(1L)).thenReturn(testJob);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.applyJob(100L, 1L, 1L, "投递自己的"); // employerId = userId
        });
        assertEquals("不能投递自己发布的职位", ex.getMessage());
    }

    // ======================== 状态流转测试 ========================

    @Test
    void testUpdateStatus_fromPendingToViewed_success() {
        when(jobApplicationMapper.selectById(1L)).thenReturn(testApplication);
        when(jobApplicationMapper.updateById(any(JobApplication.class))).thenReturn(1);

        boolean result = jobApplicationService.updateApplicationStatus(1L, 1, null);

        assertTrue(result, "待处理->已查看 应成功");
    }

    @Test
    void testUpdateStatus_fromPendingToRejected_success() {
        when(jobApplicationMapper.selectById(1L)).thenReturn(testApplication);
        when(jobApplicationMapper.updateById(any(JobApplication.class))).thenReturn(1);

        boolean result = jobApplicationService.updateApplicationStatus(1L, 4, "不符合要求");

        assertTrue(result, "待处理->已拒绝 应成功");
    }

    @Test
    void testUpdateStatus_fromRejected_shouldFail() {
        testApplication.setStatus(4); // 已拒绝（终态）
        when(jobApplicationMapper.selectById(1L)).thenReturn(testApplication);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.updateApplicationStatus(1L, 1, null);
        });
        assertTrue(ex.getMessage().contains("状态流转不合法"));
    }

    @Test
    void testUpdateStatus_fromHired_shouldFail() {
        testApplication.setStatus(3); // 已录用（终态）
        when(jobApplicationMapper.selectById(1L)).thenReturn(testApplication);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.updateApplicationStatus(1L, 4, null);
        });
        assertTrue(ex.getMessage().contains("状态流转不合法"));
    }

    @Test
    void testUpdateStatus_rejectWithoutReason_shouldFail() {
        when(jobApplicationMapper.selectById(1L)).thenReturn(testApplication);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.updateApplicationStatus(1L, 4, ""); // 拒绝但无原因
        });
        assertEquals("拒绝时必须填写拒绝原因", ex.getMessage());
    }

    @Test
    void testUpdateStatus_nonExistent_shouldThrowException() {
        when(jobApplicationMapper.selectById(999L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jobApplicationService.updateApplicationStatus(999L, 1, null);
        });
        assertEquals("投递记录不存在", ex.getMessage());
    }

    // ======================== hasApplied 测试 ========================

    @Test
    void testHasApplied_true() {
        when(jobApplicationMapper.selectCount(any())).thenReturn(1L);

        boolean result = jobApplicationService.hasApplied(200L, 1L);

        assertTrue(result, "已投递应返回 true");
    }

    @Test
    void testHasApplied_false() {
        when(jobApplicationMapper.selectCount(any())).thenReturn(0L);

        boolean result = jobApplicationService.hasApplied(200L, 1L);

        assertFalse(result, "未投递应返回 false");
    }
}
