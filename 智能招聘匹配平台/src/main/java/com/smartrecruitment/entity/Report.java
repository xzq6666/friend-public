package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 举报实体
 */
@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("reporter_id")
    private Long reporterId; // 举报人ID

    @TableField("reported_type")
    private Integer reportedType; // 1-帖子, 2-评论, 3-用户, 4-职位

    @TableField("reported_id")
    private Long reportedId; // 被举报对象ID

    @TableField("reason")
    private String reason; // 举报原因

    @TableField("description")
    private String description; // 详细描述

    @TableField("evidence_urls")
    private String evidenceUrls; // 证据图片URL列表（JSON）

    @TableField("status")
    private Integer status = 0; // 0-待处理, 1-处理中, 2-已处理, 3-已驳回

    @TableField("handler_id")
    private Long handlerId; // 处理人ID

    @TableField("handle_result")
    private String handleResult; // 处理结果

    @TableField("handle_time")
    private LocalDateTime handleTime; // 处理时间

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
