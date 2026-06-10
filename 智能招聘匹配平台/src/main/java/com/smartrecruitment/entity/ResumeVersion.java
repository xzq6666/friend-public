package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 简历版本实体
 */
@Data
@TableName("resume_version")
public class ResumeVersion {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("resume_id")
    private Long resumeId;

    @TableField("version_number")
    private String versionNumber; // v1.0, v1.1...

    @TableField("version_tag")
    private String versionTag; // 投递前/优化后等

    @TableField("version_note")
    private String versionNote;

    @TableField("snapshot_data")
    private String snapshotData; // JSON完整快照

    @TableField("skills_snapshot")
    private String skillsSnapshot; // JSON技能数组

    @TableField("work_exp_snapshot")
    private String workExpSnapshot; // JSON工作经历

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
