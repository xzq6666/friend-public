package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================
 * 简历实体 - 对应数据库表 `resume`
 * ================================================
 *
 * 字段说明：
 *   id              - 简历唯一标识
 *   userId          - 所属用户的ID
 *   name            - 候选人姓名
 *   age             - 年龄
 *   experience      - 工作经验描述（文本）
 *   skills          - 技能列表（JSON 格式，如 ["Java","Spring"]）
 *   education       - 教育背景描述
 *   expectedSalary  - 期望薪资
 *   workExperience  - 工作经历（JSON 格式）
 *   aiAnalysis      - AI 分析结果（JSON 格式存储）
 *   createTime      - 创建时间
 *   updateTime      - 更新时间
 *
 * 如需扩展：
 *   - 添加 phone/email 联系方式字段
 *   - 添加 selfEvaluation 自我评价字段
 *   - 添加 isPublic 公开/私密状态
 *   - 添加 viewCount 被查看次数
 *   - 添加 category 简历分类/标签
 */
@Data
@TableName("resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("resume_name")
    private String resumeName; // 简历名称

    @TableField("is_default")
    private Integer isDefault; // 是否默认简历：1-是，0-否

    @TableField("category_id")
    private Long categoryId;

    @TableField("name")
    private String name;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("age")
    private Integer age;

    @TableField("experience")
    private String experience;

    @TableField("skills")
    private String skills; // JSON格式存储技能

    @TableField("education")
    private String education;

    @TableField("expected_salary")
    private BigDecimal expectedSalary;

    @TableField("work_experience")
    private String workExperience; // JSON格式存储工作经验

    @TableField("self_introduction")
    private String selfIntroduction; // 自我评价

    @TableField("ai_analysis")
    private String aiAnalysis; // AI分析结果

    @TableField("file_url")
    private String fileUrl; // 附件简历文件路径/OSS URL

    @TableField("note")
    private String note; // 简历备注

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
