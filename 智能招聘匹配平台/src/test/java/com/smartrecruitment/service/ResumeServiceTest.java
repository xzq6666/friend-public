package com.smartrecruitment.service;

import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.mapper.ResumeMapper;
import com.smartrecruitment.service.impl.ResumeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResumeService 单元测试
 * 测试简历创建、查询、校验等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @InjectMocks
    private ResumeServiceImpl resumeService;

    @Mock
    private ResumeMapper resumeMapper;

    private Resume testResume;

    @BeforeEach
    void setUp() {
        testResume = new Resume();
        testResume.setId(1L);
        testResume.setUserId(100L);
        testResume.setResumeName("我的简历");
        testResume.setName("张三");
        testResume.setPhone("13800138000");
        testResume.setEmail("zhangsan@example.com");
        testResume.setAge(28);
        testResume.setExperience("3年Java开发经验");
        testResume.setSkills("[\"Java\",\"Spring\",\"MySQL\"]");
        testResume.setEducation("本科 计算机科学与技术");
        testResume.setExpectedSalary(new BigDecimal("15000"));
        testResume.setIsDefault(1);
    }

    // ======================== 简历校验测试 ========================

    @Test
    void testValidateResume_validResume_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            resumeService.validateResume(testResume);
        }, "合法简历不应抛出异常");
    }

    @Test
    void testValidateResume_nameTooLong_shouldThrowException() {
        testResume.setName("A".repeat(51)); // 超过50字符
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resumeService.validateResume(testResume);
        });
        assertTrue(ex.getMessage().contains("姓名长度不能超过50个字符"),
                   "应提示姓名过长");
    }

    @Test
    void testValidateResume_invalidAge_shouldThrowException() {
        testResume.setAge(15); // 小于16岁
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            resumeService.validateResume(testResume);
        });
        assertTrue(ex.getMessage().contains("年龄必须在16-80岁之间"),
                   "应提示年龄不合法");
    }

    // ======================== 简历 CRUD 测试 ========================

    @Test
    void testCreateResume_success() {
        when(resumeMapper.insert(any(Resume.class))).thenReturn(1);

        Resume result = resumeService.createResume(testResume);

        assertNotNull(result, "创建简历应返回结果");
        verify(resumeMapper).insert(any(Resume.class));
    }

    @Test
    void testGetResumeByUserId_shouldReturnResume() {
        when(resumeMapper.selectOne(any())).thenReturn(testResume);

        Resume result = resumeService.getResumeByUserId(100L);

        assertNotNull(result, "应返回简历");
        assertEquals("张三", result.getName());
    }

    @Test
    void testGetResumeByUserId_noResume_shouldReturnNull() {
        when(resumeMapper.selectOne(any())).thenReturn(null);

        Resume result = resumeService.getResumeByUserId(999L);

        assertNull(result, "无简历时应返回 null");
    }

    @Test
    void testGetDefaultResumeByUserId_shouldReturnDefaultResume() {
        when(resumeMapper.selectOne(any())).thenReturn(testResume);

        Resume result = resumeService.getDefaultResumeByUserId(100L);

        assertNotNull(result, "应返回默认简历");
        assertEquals(1, result.getIsDefault(), "应为默认简历");
    }

    // ======================== 统计测试 ========================

    @Test
    void testCountByDateRange_shouldReturnCorrectCount() {
        when(resumeMapper.selectCount(any())).thenReturn(15L);

        long count = resumeService.countByDateRange(
                java.time.LocalDateTime.now().minusDays(30),
                java.time.LocalDateTime.now()
        );

        assertEquals(15L, count, "按日期范围统计简历数应正确");
    }
}
