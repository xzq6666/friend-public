package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ================================================
 * 通知实体 - 对应数据库表 `notification`
 * ================================================
 *
 * 字段说明：
 *   id         - 主键
 *   userId     - 接收用户ID
 *   title      - 标题
 *   content    - 内容
 *   type       - 类型: job_match/interview/system
 *   isRead     - 是否已读 (0/1)
 *   createTime - 创建时间
 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("type")
    private String type; // job_match, interview, system

    @TableField("is_read")
    private Integer isRead = 0;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
