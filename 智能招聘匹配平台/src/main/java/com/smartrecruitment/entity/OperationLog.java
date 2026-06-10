package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("operation")
    private String operation; // 操作类型：登录/注册/创建/更新/删除

    @TableField("module")
    private String module; // 操作模块：用户/简历/职位/匹配

    @TableField("description")
    private String description; // 操作描述

    @TableField("ip_address")
    private String ipAddress;

    @TableField("status")
    private Integer status; // 0-失败 1-成功

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
