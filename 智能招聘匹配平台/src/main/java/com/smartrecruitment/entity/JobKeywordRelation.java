package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位-关键词关联实体
 */
@Data
@TableName("job_keyword_relation")
public class JobKeywordRelation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("keyword_id")
    private Long keywordId;

    @TableField("weight")
    private Integer weight = 5;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
