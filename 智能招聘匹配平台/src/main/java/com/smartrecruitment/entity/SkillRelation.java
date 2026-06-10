package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 技能关系实体 - 存储技能之间的关联关系
 */
@Data
@TableName("skill_relation")
public class SkillRelation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("source_id")
    private Long sourceId; // 源技能ID

    @TableField("target_id")
    private Long targetId; // 目标技能ID

    @TableField("relation_type")
    private String relationType; // 关系类型：依赖/相关/同类别

    @TableField("weight")
    private Integer weight; // 关系权重：1-10

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
