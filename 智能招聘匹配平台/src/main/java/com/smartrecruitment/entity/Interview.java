package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smartrecruitment.config.LocalDateTimeDeserializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 面试邀请实体
 */
@Data
@TableName("interview")
public class Interview {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("application_id")
    private Long applicationId; // 关联投递记录ID

    @TableField("job_id")
    private Long jobId; // 职位ID

    @TableField("user_id")
    private Long userId; // 求职者ID

    @TableField("employer_id")
    private Long employerId; // 企业ID

    @TableField("resume_id")
    private Long resumeId; // 简历ID（人才市场直接沟通时使用）

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @TableField("interview_time")
    private LocalDateTime interviewTime; // 面试时间

    @TableField("interview_location")
    private String interviewLocation; // 面试地点

    @TableField("interview_type")
    private Integer interviewType; // 面试类型：1-现场面试 2-视频面试 3-电话面试

    @TableField("contact_person")
    private String contactPerson; // 联系人

    @TableField("contact_phone")
    private String contactPhone; // 联系电话

    @TableField("notes")
    private String notes; // 面试备注

    @TableField("chat_type")
    private Integer chatType; // 类型：0-正式面试邀请 1-直接沟通（聊天）

    @TableField("status")
    private Integer status; // 状态：0-待确认 1-已确认 2-已取消 3-已完成

    @TableField("deleted_by_user")
    private Integer deletedByUser; // 求职者是否删除该对话(0:否 1:是)

    @TableField("deleted_by_employer")
    private Integer deletedByEmployer; // 企业用户是否删除该对话(0:否 1:是)

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
