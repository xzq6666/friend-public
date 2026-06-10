package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 投递记录实体 - 记录求职者投递简历到职位的记录
 */
@Data
@TableName("job_application")
public class JobApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("resume_id")
    private Long resumeId; // 简历ID

    @TableField("job_id")
    private Long jobId; // 职位ID

    @TableField("user_id")
    private Long userId; // 投递用户ID

    @TableField("status")
    private Integer status; // 状态：0-待处理 1-已查看 2-邀请面试 3-已录用 4-已拒绝

    @TableField("cover_letter")
    private String coverLetter; // 求职信

    @TableField("reject_reason")
    private String rejectReason; // 拒绝原因

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
