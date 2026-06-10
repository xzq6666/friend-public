package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ================================================
 * 用户实体 - 对应数据库表 `user`
 * ================================================
 *
 * 字段说明：
 *   id         - 用户唯一标识，自增主键
 *   username   - 用户名，唯一登录凭证
 *   password   - BCrypt 加密后的密码
 *   email      - 邮箱，可选
 *   phone      - 手机号，可选
 *   userType   - 用户类型：EMPLOYEE(求职者) / EMPLOYER(企业)
 *   status     - 账户状态：1-正常 0-禁用
 *   createTime - 注册时间
 *   updateTime - 更新时间
 *
 * 如需扩展：
 *   - 添加 avatar 字段存储用户头像 URL
 *   - 添加 gender/age 等个人信息字段
 *   - 添加 lastLoginTime 记录最后登录时间
 *   - 添加 role 字段实现角色权限分离（如 ADMIN 管理员角色）
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("user_type")
    private String userType; // EMPLOYEE, EMPLOYER

    @TableField("avatar")
    private String avatar; // 头像URL

    @TableField("status")
    private Integer status = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
