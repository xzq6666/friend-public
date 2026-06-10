package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 访问历史记录实体
 */
@Data
@TableName("visit_history")
public class VisitHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("visitor_id")
    private Long visitorId; // 访问者用户ID

    @TableField("visitor_username")
    private String visitorUsername; // 访问者用户名快照

    @TableField("visitor_user_type")
    private String visitorUserType; // 访问者类型: EMPLOYEE/EMPLOYER

    @TableField("visitor_avatar")
    private String visitorAvatar; // 访问者头像快照

    @TableField("target_type")
    private Integer targetType; // 访问类型：1=简历/主页 2=职位

    @TableField("target_id")
    private Long targetId; // 目标ID(简历ID或职位ID)

    @TableField("target_owner_id")
    private Long targetOwnerId; // 被访问内容的所有者ID

    @TableField("is_anonymous")
    private Integer isAnonymous; // 是否匿名访问(0否/1是)

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
