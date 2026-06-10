package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("publisher_id")
    private Long publisherId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("target_type")
    private String targetType; // ALL / EMPLOYEE / EMPLOYER

    @TableField("is_active")
    private Integer isActive = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
