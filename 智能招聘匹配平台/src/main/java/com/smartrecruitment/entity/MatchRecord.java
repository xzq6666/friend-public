package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================
 * 匹配记录实体 - 对应数据库表 `match_record`
 * ================================================
 *
 * 字段说明：
 *   id           - 匹配记录唯一标识
 *   resumeId     - 关联的简历ID
 *   jobId        - 关联的职位ID
 *   matchScore   - AI 计算的匹配度分数（0-100）
 *   aiSuggestion - AI 给出的建议（文本）
 *   status       - 处理状态：0-待处理 1-已查看 2-已邀请面试
 *   createTime   - 匹配时间
 *
 * 如需扩展：
 *   - 添加 resumeName/jobTitle 冗余字段，方便列表展示
 *   - 添加 matchDetail 存储详细的匹配维度分析（JSON）
 *   - 添加 operatorId 操作人ID
 *   - 添加 interviewTime/interviewAddress 面试安排字段
 *   - 添加 remark 备注字段
 */
@Data
@TableName("match_record")
public class MatchRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("resume_id")
    private Long resumeId;

    @TableField("job_id")
    private Long jobId;

    @TableField("match_score")
    private BigDecimal matchScore; // AI匹配度分数

    @TableField("ai_suggestion")
    private String aiSuggestion; // AI建议

    @TableField("match_detail")
    private String matchDetail; // 各维度得分详情（JSON）

    @TableField("status")
    private Integer status = 0; // 0:待处理, 1:已查看, 2:已邀请面试

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
