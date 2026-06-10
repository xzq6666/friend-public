package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ================================================
 * 通知偏好实体 - 对应数据库表 `notification_prefs`
 * ================================================
 *
 * 字段说明：
 *   id              - 主键
 *   userId          - 用户ID
 *   emailNotify     - 邮件通知开关 (0/1)
 *   smsNotify       - 短信通知开关 (0/1)
 *   jobMatchNotify  - 职位匹配通知开关 (0/1)
 *   interviewNotify - 面试通知开关 (0/1)
 *   systemNotify    - 系统通知开关 (0/1)
 *   chatNotify      - 聊天通知开关 (0/1)
 *   createTime      - 创建时间
 *   updateTime      - 更新时间
 */
@Data
@TableName("notification_prefs")
public class NotificationPreference {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("email_notify")
    private Integer emailNotify = 1;

    @TableField("sms_notify")
    private Integer smsNotify = 1;

    @TableField("job_match_notify")
    private Integer jobMatchNotify = 1;

    @TableField("interview_notify")
    private Integer interviewNotify = 1;

    @TableField("system_notify")
    private Integer systemNotify = 1;

    @TableField("chat_notify")
    private Integer chatNotify = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
