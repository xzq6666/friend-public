package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 匹配屏蔽记录
 * 用户可屏蔽特定职位（求职者）或特定候选人（企业），屏蔽后不再出现在推荐列表中
 */
@TableName("match_block")
public class MatchBlock {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID */
    private Long userId;

    /** 屏蔽类型：1=求职者屏蔽职位, 2=企业屏蔽候选人 */
    private Integer blockType;

    /** 被屏蔽的职位ID（求职者屏蔽职位时） */
    private Long jobId;

    /** 被屏蔽的简历/候选人ID（企业屏蔽候选人时） */
    private Long resumeId;

    /** 屏蔽原因（可选） */
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public MatchBlock() {}

    public MatchBlock(Long userId, Integer blockType, Long jobId, Long resumeId, String reason) {
        this.userId = userId;
        this.blockType = blockType;
        this.jobId = jobId;
        this.resumeId = resumeId;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getBlockType() { return blockType; }
    public void setBlockType(Integer blockType) { this.blockType = blockType; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
