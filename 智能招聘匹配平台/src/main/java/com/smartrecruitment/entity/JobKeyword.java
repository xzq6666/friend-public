package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位关键词实体
 */
@Data
@TableName("job_keyword")
public class JobKeyword {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("keyword")
    private String keyword;

    @TableField("category")
    private String category;

    @TableField("source")
    private String source;

    @TableField("usage_count")
    private Integer usageCount = 0;

    @TableField("is_active")
    private Integer isActive = 1;

    @TableField("description")
    private String description;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
