package com.smartrecruitment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.entity.CompanyInfo;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JobController 集成测试
 * 测试职位相关 HTTP 接口
 */
@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @MockBean
    private UserService userService;

    @MockBean
    private CompanyInfoService companyInfoService;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private OperationLogService operationLogService;

    @MockBean
    private VisitHistoryService visitHistoryService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JobApplicationService jobApplicationService;

    @MockBean
    private SensitiveWordService sensitiveWordService;

    @MockBean
    private JobSubscriptionService jobSubscriptionService;

    private User testUser;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("employer1");
        testUser.setUserType("EMPLOYER");
        testUser.setStatus(1);

        testJob = new Job();
        testJob.setId(1L);
        testJob.setEmployerId(1L);
        testJob.setTitle("Java开发工程师");
        testJob.setDescription("负责后端开发");
        testJob.setRequirements("3年以上Java经验");
        testJob.setSalaryMin(new BigDecimal("15000"));
        testJob.setSalaryMax(new BigDecimal("25000"));
        testJob.setLocation("北京");
        testJob.setStatus(1);
    }

    // ======================== 获取职位详情测试 ========================

    @Test
    void testGetJob_exists_shouldReturnJob() throws Exception {
        when(jobService.findById(1L)).thenReturn(testJob);
        when(companyInfoService.getByUserId(1L)).thenReturn(null);
        when(favoriteService.isFavorited(anyLong(), anyInt(), anyLong())).thenReturn(false);

        mockMvc.perform(get("/job/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java开发工程师"))
                .andExpect(jsonPath("$.location").value("北京"));
    }

    @Test
    void testGetJob_notExists_shouldReturn404() throws Exception {
        when(jobService.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/job/999"))
                .andExpect(status().isNotFound());
    }

    // ======================== 创建职位测试 ========================

    @Test
    void testCreateJob_noAuth_shouldReturnError() throws Exception {
        // 模拟未认证用户（Authentication 为 null）
        mockMvc.perform(post("/job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJob)))
                .andExpect(status().isBadRequest());
    }

    // ======================== 更新职位状态测试 ========================

    @Test
    void testUpdateJobStatus_success() throws Exception {
        when(userService.findByUsername(anyString())).thenReturn(testUser);
        when(jobService.updateJobStatus(eq(1L), eq(0))).thenReturn(true);

        mockMvc.perform(put("/job/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk());
    }

    // ======================== 删除职位测试 ========================

    @Test
    void testDeleteJob_hasApplications_shouldReturnError() throws Exception {
        when(userService.findByUsername(anyString())).thenReturn(testUser);
        doThrow(new RuntimeException("该职位已有投递记录，不能直接删除。请先下线职位。"))
                .when(jobService).deleteJob(1L);

        mockMvc.perform(delete("/job/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("该职位已有投递记录，不能直接删除。请先下线职位。"));
    }

    @Test
    void testDeleteJob_success() throws Exception {
        when(userService.findByUsername(anyString())).thenReturn(testUser);
        when(jobService.deleteJob(1L)).thenReturn(true);

        mockMvc.perform(delete("/job/1"))
                .andExpect(status().isOk());
    }
}
