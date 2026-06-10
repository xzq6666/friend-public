package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 技能图谱实体 - 存储技能节点和关系
 */
@Data
@TableName("skill_graph")
public class SkillGraph {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name; // 技能名称

    @TableField("category")
    private String category; // 技能分类：语言/框架/工具/软技能等

    @TableField("industry")
    private String industry; // 所属行业：IT/互联网、金融/财务、医疗/健康 等

    @TableField("level")
    private Integer level; // 技能等级：1-5

    @TableField("parent_id")
    private Long parentId; // 父技能ID，用于构建层级关系

    @TableField("description")
    private String description; // 技能描述

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
