package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏记录实体 - 收藏职位或简历
 */
@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId; // 收藏用户ID

    @TableField("target_type")
    private Integer targetType; // 收藏类型：1-职位 2-简历

    @TableField("target_id")
    private Long targetId; // 收藏目标ID

    @TableField("folder_id")
    private Long folderId; // 所属文件夹ID，可为空

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
