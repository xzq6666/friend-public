package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户隐私设置实体
 */
@Data
@TableName("user_privacy_settings")
public class UserPrivacySettings {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId; // 用户ID

    @TableField("default_anonymous")
    private Integer defaultAnonymous; // 默认匿名访问(0否/1是)

    @TableField("show_visit_history")
    private Integer showVisitHistory; // 是否显示访问记录(0隐藏/1显示)

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
