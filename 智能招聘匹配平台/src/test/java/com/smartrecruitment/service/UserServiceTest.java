package com.smartrecruitment.service;

import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.UserMapper;
import com.smartrecruitment.service.impl.UserServiceImpl;
import com.smartrecruitment.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 * 使用 Mockito 模拟数据库操作，测试用户注册、登录、查询等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setUserType("EMPLOYEE");
        testUser.setStatus(1);
    }

    // ======================== 注册测试 ========================

    @Test
    void testRegister_success() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("new@example.com");
        newUser.setUserType("EMPLOYEE");

        // 模拟用户名不存在
        when(userMapper.selectOne(any())).thenReturn(null);
        // 模拟邮箱不重复
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        boolean result = userService.register(newUser);

        assertTrue(result, "注册应返回 true");
        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testRegister_duplicateUsername_shouldThrowException() {
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser");
        duplicateUser.setPassword("password123");

        // 模拟用户名已存在
        when(userMapper.selectOne(any())).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(duplicateUser);
        });
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void testRegister_duplicateEmail_shouldThrowException() {
        User duplicateUser = new User();
        duplicateUser.setUsername("anotheruser");
        duplicateUser.setPassword("password123");
        duplicateUser.setEmail("test@example.com");

        // 模拟用户名不存在但邮箱已存在
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(duplicateUser);
        });
        assertEquals("邮箱已被注册", exception.getMessage());
    }

    @Test
    void testRegister_invalidUserType_shouldDefaultToEmployee() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password123");
        newUser.setUserType("ADMIN"); // 试图注册为管理员

        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register(newUser);

        assertEquals("EMPLOYEE", newUser.getUserType(),
                "非法 userType 应被强制设为 EMPLOYEE");
    }

    // ======================== 登录测试 ========================

    @Test
    void testLogin_success() {
        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("1", "testuser")).thenReturn("mock-token");

        String token = userService.login("testuser", "password123");

        assertNotNull(token, "登录成功应返回 Token");
        assertEquals("mock-token", token);
    }

    @Test
    void testLogin_wrongPassword_shouldReturnNull() {
        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encodedPassword")).thenReturn(false);

        String token = userService.login("testuser", "wrongpassword");

        assertNull(token, "密码错误时应返回 null");
    }

    @Test
    void testLogin_nonExistentUser_shouldReturnNull() {
        when(userMapper.selectOne(any())).thenReturn(null);

        String token = userService.login("nonexistent", "password123");

        assertNull(token, "用户不存在时应返回 null");
    }

    @Test
    void testLogin_disabledUser_shouldThrowException() {
        User disabledUser = new User();
        disabledUser.setId(2L);
        disabledUser.setUsername("disabled");
        disabledUser.setPassword("$2a$10$encoded");
        disabledUser.setStatus(0); // 禁用状态

        when(userMapper.selectOne(any())).thenReturn(disabledUser);
        when(passwordEncoder.matches("password", "$2a$10$encoded")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login("disabled", "password");
        });
        assertTrue(exception.getMessage().contains("禁用"), "禁用用户登录应抛出异常");
    }

    // ======================== 查询测试 ========================

    @Test
    void testCountByType_shouldReturnCorrectCount() {
        when(userMapper.selectCount(any())).thenReturn(10L);

        long count = userService.countByType("EMPLOYEE");

        assertEquals(10L, count, "按类型统计数量应正确");
    }

    @Test
    void testCountByDateRange_shouldReturnCorrectCount() {
        when(userMapper.selectCount(any())).thenReturn(5L);

        long count = userService.countByDateRange(
                java.time.LocalDateTime.now().minusDays(7),
                java.time.LocalDateTime.now()
        );

        assertEquals(5L, count, "按日期范围统计数量应正确");
    }

    @Test
    void testCountActiveUsers_shouldReturnCorrectCount() {
        when(userMapper.selectCount(any())).thenReturn(20L);

        long count = userService.countActiveUsers(java.time.LocalDateTime.now().minusDays(30));

        assertEquals(20L, count, "活跃用户统计应正确");
    }
}
