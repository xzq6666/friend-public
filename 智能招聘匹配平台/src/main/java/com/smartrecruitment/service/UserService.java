package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.User;

import java.time.LocalDateTime;

/**
 * ================================================
 * 用户服务接口
 * ================================================
 *
 * 提供用户相关的业务逻辑抽象。
 * 继承 MyBatis-Plus IService 获得基础 CRUD 方法。
 *
 * 如需扩展：
 *   - 添加 updateProfile(User user) 更新用户资料
 *   - 添加 changePassword(oldPwd, newPwd) 修改密码
 *   - 添加 resetPassword(email) 重置密码
 *   - 添加 findByEmail(String email) 邮箱查找用户
 *   - 添加 batchDelete(List<Long> ids) 批量删除用户
 *   - 添加 getStatistics() 用户统计数据
 *   - 添加 checkUsernameExists(String username) 用户名重名校验
 */
public interface UserService extends IService<User> {
    User findByUsername(String username);
    String login(String username, String password);
    boolean register(User user);

    // 数据统计方法
    long countByType(String userType);
    long countByCreateTime(LocalDateTime createTime);
    long countByDateRange(LocalDateTime start, LocalDateTime end);
    long countActiveUsers(LocalDateTime since);
}
