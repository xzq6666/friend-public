package com.smartrecruitment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.*;
import com.smartrecruitment.utils.IpUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 * 测试用户注册和登录接口的完整 HTTP 请求/响应流程
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 关闭安全过滤器以便直接测试
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private OperationLogService operationLogService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JobService jobService;

    @MockBean
    private ResumeService resumeService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setUserType("EMPLOYEE");
        testUser.setStatus(1);
    }

    // ======================== 注册接口测试 ========================

    @Test
    void testRegister_success() throws Exception {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("new@example.com");
        newUser.setUserType("EMPLOYEE");

        when(userService.register(any(User.class))).thenReturn(true);
        when(userService.findByUsername("newuser")).thenReturn(newUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    @Test
    void testRegister_duplicateUsername_shouldReturn400() throws Exception {
        User duplicateUser = new User();
        duplicateUser.setUsername("existinguser");
        duplicateUser.setPassword("password123");

        when(userService.register(any(User.class)))
                .thenThrow(new RuntimeException("用户名已存在"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("用户名已存在"));
    }

    @Test
    void testRegister_serviceFailure_shouldReturn400() throws Exception {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");

        when(userService.register(any(User.class))).thenReturn(false);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("注册失败"));
    }

    // ======================== 登录接口测试 ========================

    @Test
    void testLogin_success() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "password123");

        when(userService.login("testuser", "password123")).thenReturn("mock-jwt-token");
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.userType").value("EMPLOYEE"))
                .andExpect(jsonPath("$.user.id").value(1));
    }

    @Test
    void testLogin_wrongPassword_shouldReturn400() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "wrongpassword");

        when(userService.login("testuser", "wrongpassword")).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("用户名或密码错误"));
    }

    @Test
    void testLogin_emptyUsername_shouldReturn400() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "");
        loginRequest.put("password", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("用户名和密码不能为空"));
    }

    @Test
    void testLogin_nullPassword_shouldReturn400() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        // password 未设置，为 null

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("用户名和密码不能为空"));
    }

    @Test
    void testLogin_disabledAccount_shouldReturn400() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "disableduser");
        loginRequest.put("password", "password123");

        when(userService.login("disableduser", "password123"))
                .thenThrow(new RuntimeException("该账户已被禁用，请联系管理员"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("该账户已被禁用，请联系管理员"));
    }

    // ======================== 公开统计接口测试 ========================

    @Test
    void testGetPublicStats_shouldReturnStats() throws Exception {
        when(jobService.count()).thenReturn(100L);
        when(userService.count()).thenReturn(500L);
        when(resumeService.count()).thenReturn(300L);

        mockMvc.perform(get("/auth/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobCount").value(100))
                .andExpect(jsonPath("$.userCount").value(500))
                .andExpect(jsonPath("$.resumeCount").value(300));
    }
}
