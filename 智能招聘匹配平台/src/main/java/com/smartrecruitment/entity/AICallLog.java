package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI调用日志 - 记录每次AI API调用的详情
 * 用于：监控AI质量、分析token消耗、排查问题、A/B测试
 */
@Data
@TableName("ai_call_log")
public class AICallLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调用类型：RESUME_ANALYZE/MATCH_SCORE/SEMANTIC_MATCH/INTERVIEW/RECOMMEND/COVER_LETTER/RESUME_OPTIMIZE/SALARY_PREDICT/CAREER_PATH/RESUME_COMPARE/DOCUMENT_PARSE */
    @TableField("call_type")
    private String callType;

    /** 关联的用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联的简历ID */
    @TableField("resume_id")
    private Long resumeId;

    /** 关联的职位ID */
    @TableField("job_id")
    private Long jobId;

    /** 使用的模型 */
    @TableField("model")
    private String model;

    /** Prompt长度（字符数） */
    @TableField("prompt_length")
    private Integer promptLength;

    /** 响应长度（字符数） */
    @TableField("response_length")
    private Integer responseLength;

    /** Token用量 - prompt tokens */
    @TableField("prompt_tokens")
    private Integer promptTokens;

    /** Token用量 - completion tokens */
    @TableField("completion_tokens")
    private Integer completionTokens;

    /** Token用量 - total */
    @TableField("total_tokens")
    private Integer totalTokens;

    /** 调用耗时（毫秒） */
    @TableField("elapsed_ms")
    private Long elapsedMs;

    /** 调用状态：SUCCESS/FAILED/TIMEOUT */
    @TableField("status")
    private String status;

    /** 错误信息 */
    @TableField("error_message")
    private String errorMessage;

    /** AI返回结果的摘要（截取前200字符） */
    @TableField("response_summary")
    private String responseSummary;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
