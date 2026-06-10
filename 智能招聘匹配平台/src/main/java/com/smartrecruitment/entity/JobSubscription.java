package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 职位订阅实体
 */
@Data
@TableName("job_subscription")
public class JobSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("name")
    private String name;

    @TableField("keywords")
    private String keywords; // JSON数组

    @TableField("category_id")
    private Long categoryId;

    @TableField("salary_min")
    private BigDecimal salaryMin;

    @TableField("salary_max")
    private BigDecimal salaryMax;

    @TableField("location")
    private String location;

    @TableField("experience_required")
    private String experienceRequired;

    @TableField("education_required")
    private String educationRequired;

    @TableField("job_type")
    private String jobType;

    @TableField("company_scale")
    private String companyScale;

    @TableField("push_strategy")
    private String pushStrategy; // realtime/daily/weekly

    @TableField("is_active")
    private Integer isActive = 1;

    @TableField("last_push_time")
    private LocalDateTime lastPushTime;

    @TableField("match_count")
    private Integer matchCount = 0;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
