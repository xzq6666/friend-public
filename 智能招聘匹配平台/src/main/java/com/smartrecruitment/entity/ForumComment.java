package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_comment")
public class ForumComment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("user_id")
    private Long userId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("reply_to_user_id")
    private Long replyToUserId;

    @TableField("content")
    private String content;

    @TableField("status")
    private Integer status = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 非数据库字段：评论人用户名 */
    @TableField(exist = false)
    private String username;

    /** 非数据库字段：评论人头像 */
    @TableField(exist = false)
    private String avatar;

    /** 非数据库字段：评论人类型 */
    @TableField(exist = false)
    private String userType;

    /** 非数据库字段：被回复人用户名 */
    @TableField(exist = false)
    private String replyToUsername;

    /** 非数据库字段：子评论列表 */
    @TableField(exist = false)
    private java.util.List<ForumComment> children;
}
