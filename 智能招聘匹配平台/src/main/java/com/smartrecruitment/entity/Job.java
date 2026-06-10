package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================
 * 职位实体 - 对应数据库表 `job`
 * ================================================
 *
 * 字段说明：
 *   id                - 职位唯一标识
 *   employerId        - 发布该职位的企业用户ID
 *   title             - 职位名称（如 "Java后端工程师"）
 *   description       - 职位描述（文本）
 *   requirements      - 任职要求（文本）
 *   salaryMin         - 最低薪资
 *   salaryMax         - 最高薪资
 *   location          - 工作地点（如 "北京"）
 *   experienceRequired- 经验要求（如 "3-5年"）
 *   educationRequired - 学历要求（如 "本科"）
 *   status            - 发布状态：1-发布中 0-已下线
 *   createTime        - 发布时间
 *   updateTime        - 更新时间
 *
 * 如需扩展：
 *   - 添加 department 部门字段
 *   - 添加 jobType 工作类型（全职/兼职/实习）
 *   - 添加 welfare 福利标签（五险一金/双休等）
 *   - 添加 contactPerson/contactPhone 联系人信息
 *   - 添加 viewCount/applyCount 统计字段
 *   - 添加 category 职位分类用于搜索筛选
 */
@Data
@TableName("job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("employer_id")
    private Long employerId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("requirements")
    private String requirements;

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

    @TableField("work_type")
    private String workType;

    @TableField("status")
    private Integer status = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
