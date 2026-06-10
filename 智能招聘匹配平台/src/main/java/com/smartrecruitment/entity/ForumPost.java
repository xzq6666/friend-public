package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_post")
public class ForumPost {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("is_pinned")
    private Integer isPinned = 0;

    @TableField("is_closed")
    private Integer isClosed = 0;

    @TableField("comment_count")
    private Integer commentCount = 0;

    @TableField("status")
    private Integer status = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 非数据库字段：发帖人用户名 */
    @TableField(exist = false)
    private String username;

    /** 非数据库字段：发帖人头像 */
    @TableField(exist = false)
    private String avatar;

    /** 非数据库字段：发帖人类型 */
    @TableField(exist = false)
    private String userType;
}
