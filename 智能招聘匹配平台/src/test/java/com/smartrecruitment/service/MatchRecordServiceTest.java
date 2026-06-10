package com.smartrecruitment.service;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.MatchRecord;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.mapper.MatchRecordMapper;
import com.smartrecruitment.service.impl.MatchRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MatchRecordService 单元测试
 * 测试匹配记录创建、查询、状态更新、匹配计算等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class MatchRecordServiceTest {

    @InjectMocks
    private MatchRecordServiceImpl matchRecordService;

    @Mock
    private MatchRecordMapper matchRecordMapper;

    @Mock
    private ResumeService resumeService;

    @Mock
    private JobService jobService;

    @Mock
    private AsyncMatchService asyncMatchService;

    @Mock
    private JobKeywordService jobKeywordService;

    @Mock
    private AIService aiService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private MatchRecord testMatchRecord;
    private Resume testResume;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testMatchRecord = new MatchRecord();
        testMatchRecord.setId(1L);
        testMatchRecord.setResumeId(1L);
        testMatchRecord.setJobId(1L);
        testMatchRecord.setMatchScore(new BigDecimal("75.5"));
        testMatchRecord.setAiSuggestion("匹配度较高，建议面试");
        testMatchRecord.setStatus(0);

        testResume = new Resume();
        testResume.setId(1L);
        testResume.setUserId(100L);
        testResume.setName("张三");
        testResume.setSkills("[\"Java\",\"Spring\",\"MySQL\"]");
        testResume.setExperience("3年Java开发经验");
        testResume.setEducation("本科");
        testResume.setExpectedSalary(new BigDecimal("15000"));

        testJob = new Job();
        testJob.setId(1L);
        testJob.setEmployerId(200L);
        testJob.setTitle("Java后端工程师");
        testJob.setDescription("负责后端开发");
        testJob.setRequirements("3年以上Java开发经验，熟悉Spring框架");
        testJob.setSalaryMin(new BigDecimal("12000"));
        testJob.setSalaryMax(new BigDecimal("20000"));
        testJob.setLocation("北京");
        testJob.setEducationRequired("本科");
    }

    // ======================== 创建匹配记录测试 ========================

    @Test
    void testCreateMatchRecord_success() {
        when(matchRecordMapper.insert(any(MatchRecord.class))).thenReturn(1);

        MatchRecord result = matchRecordService.createMatchRecord(testMatchRecord);

        assertNotNull(result, "创建匹配记录应返回记录");
        assertEquals(1L, result.getResumeId());
        assertEquals(1L, result.getJobId());
        verify(matchRecordMapper).insert(any(MatchRecord.class));
    }

    // ======================== 状态更新测试 ========================

    @Test
    void testUpdateMatchStatus_success() {
        when(matchRecordMapper.selectById(1L)).thenReturn(testMatchRecord);
        when(matchRecordMapper.updateById(any(MatchRecord.class))).thenReturn(1);

        boolean result = matchRecordService.updateMatchStatus(1L, 1);

        assertTrue(result, "更新匹配状态应成功");
    }

    @Test
    void testUpdateMatchStatus_nonExistent_shouldReturnFalse() {
        when(matchRecordMapper.selectById(999L)).thenReturn(null);

        boolean result = matchRecordService.updateMatchStatus(999L, 1);

        assertFalse(result, "不存在的记录应返回 false");
    }

    // ======================== 匹配计算测试 ========================

    @Test
    void testCalculateMatchDetail_shouldReturnResult() {
        Map<String, Object> result = matchRecordService.calculateMatchDetail(testResume, testJob);

        assertNotNull(result, "匹配计算结果不应为 null");
        // 应包含匹配分数
        assertTrue(result.containsKey("matchScore") || result.containsKey("totalScore"),
                "结果应包含匹配分数");
    }

    @Test
    void testCalculateMatchDetail_withNullResume_shouldHandleGracefully() {
        // 空简历应能正常处理（返回低分或空结果）
        Map<String, Object> result = matchRecordService.calculateMatchDetail(null, testJob);

        assertNotNull(result, "空简历应返回结果而非 null");
    }

    @Test
    void testCalculateMatchDetail_withNullJob_shouldHandleGracefully() {
        // 空职位应能正常处理
        Map<String, Object> result = matchRecordService.calculateMatchDetail(testResume, null);

        assertNotNull(result, "空职位应返回结果而非 null");
    }

    // ======================== 统计测试 ========================

    @Test
    void testCountByDateRange_shouldReturnCorrectCount() {
        when(matchRecordMapper.selectCount(any())).thenReturn(25L);

        long count = matchRecordService.countByDateRange(
                java.time.LocalDateTime.now().minusDays(7),
                java.time.LocalDateTime.now()
        );

        assertEquals(25L, count, "按日期范围统计匹配记录数应正确");
    }
}
