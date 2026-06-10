package com.smartrecruitment.service;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.mapper.CompanyInfoMapper;
import com.smartrecruitment.mapper.JobApplicationMapper;
import com.smartrecruitment.mapper.JobMapper;
import com.smartrecruitment.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JobService 单元测试
 * 测试职位创建、更新、删除、校验、浏览量统计等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @InjectMocks
    private JobServiceImpl jobService;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private JobApplicationMapper jobApplicationMapper;

    @Mock
    private CompanyInfoMapper companyInfoMapper;

    @Mock
    private UserService userService;

    @Mock
    private JobKeywordService jobKeywordService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private Job testJob;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId(1L);
        testJob.setEmployerId(100L);
        testJob.setTitle("Java后端工程师");
        testJob.setDescription("负责后端开发工作");
        testJob.setRequirements("3年以上Java开发经验");
        testJob.setSalaryMin(new BigDecimal("10000"));
        testJob.setSalaryMax(new BigDecimal("20000"));
        testJob.setLocation("北京");
        testJob.setExperienceRequired("3-5年");
        testJob.setEducationRequired("本科");
        testJob.setWorkType("onsite");
        testJob.setStatus(1);
    }

    // ======================== 职位校验测试 ========================

    @Test
    void testValidateJob_validJob_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            jobService.validateJob(testJob);
        }, "合法职位不应抛出异常");
    }

    @Test
    void testValidateJob_emptyTitle_shouldThrowException() {
        testJob.setTitle("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("职位名称不能为空"), "应提示职位名称不能为空");
    }

    @Test
    void testValidateJob_nullTitle_shouldThrowException() {
        testJob.setTitle(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("职位名称不能为空"));
    }

    @Test
    void testValidateJob_titleTooLong_shouldThrowException() {
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 101; i++) longTitle.append("A");
        testJob.setTitle(longTitle.toString());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("职位名称不能超过100个字符"));
    }

    @Test
    void testValidateJob_emptyDescription_shouldThrowException() {
        testJob.setDescription("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("职位描述不能为空"));
    }

    @Test
    void testValidateJob_emptyRequirements_shouldThrowException() {
        testJob.setRequirements("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("职位要求不能为空"));
    }

    @Test
    void testValidateJob_salaryMinGreaterThanMax_shouldThrowException() {
        testJob.setSalaryMin(new BigDecimal("30000"));
        testJob.setSalaryMax(new BigDecimal("10000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("最低薪资不能高于最高薪资"));
    }

    @Test
    void testValidateJob_negativeSalary_shouldThrowException() {
        testJob.setSalaryMin(new BigDecimal("-1000"));
        testJob.setSalaryMax(new BigDecimal("10000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("薪资不能为负数"));
    }

    @Test
    void testValidateJob_nullEmployerId_shouldThrowException() {
        testJob.setEmployerId(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            jobService.validateJob(testJob);
        });
        assertTrue(ex.getMessage().contains("必须指定发布者"));
    }

    // ======================== 浏览量测试 ========================

    @Test
    void testIncrementViewCount_shouldCallRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        jobService.incrementViewCount(1L);

        verify(valueOperations).increment("job:view:1");
    }

    @Test
    void testGetViewCount_withCachedValue_shouldReturnValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("job:view:1")).thenReturn(42L);

        Long count = jobService.getViewCount(1L);

        assertEquals(42L, count, "应返回 Redis 缓存的浏览量");
    }

    @Test
    void testGetViewCount_withNoCachedValue_shouldReturnZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("job:view:1")).thenReturn(null);

        Long count = jobService.getViewCount(1L);

        assertEquals(0L, count, "无缓存时应返回 0");
    }

    @Test
    void testGetViewCount_redisException_shouldReturnZero() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis连接失败"));

        Long count = jobService.getViewCount(1L);

        assertEquals(0L, count, "Redis异常时应返回 0");
    }

    // ========================  findById 缓存测试 ========================

    @Test
    void testFindById_withCacheHit_shouldReturnCachedJob() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("job:detail:1")).thenReturn(testJob);

        Job result = jobService.findById(1L);

        assertNotNull(result, "缓存命中时应返回职位");
        assertEquals("Java后端工程师", result.getTitle());
        verify(jobMapper, never()).selectById(anyLong());
    }

    @Test
    void testFindById_withCacheMiss_shouldQueryDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("job:detail:1")).thenReturn(null);
        when(jobMapper.selectById(1L)).thenReturn(testJob);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        Job result = jobService.findById(1L);

        assertNotNull(result, "数据库查询命中时应返回职位");
        verify(jobMapper).selectById(1L);
    }

    @Test
    void testFindById_notExists_shouldReturnNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("job:detail:999")).thenReturn(null);
        when(jobMapper.selectById(999L)).thenReturn(null);

        Job result = jobService.findById(999L);

        assertNull(result, "职位不存在时应返回 null");
    }

    // ======================== 状态更新测试 ========================

    @Test
    void testUpdateJobStatus_success() {
        when(jobMapper.selectById(1L)).thenReturn(testJob);
        when(jobMapper.updateById(any(Job.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = jobService.updateJobStatus(1L, 0); // 下架

        assertTrue(result, "状态更新应成功");
    }

    @Test
    void testUpdateJobStatus_nonExistentJob_shouldReturnFalse() {
        when(jobMapper.selectById(999L)).thenReturn(null);

        boolean result = jobService.updateJobStatus(999L, 0);

        assertFalse(result, "不存在的职位更新状态应返回 false");
    }
}
