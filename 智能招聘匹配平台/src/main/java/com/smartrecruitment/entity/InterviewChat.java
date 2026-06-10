package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 面试聊天消息实体
 */
@Data
@TableName("interview_chat")
public class InterviewChat {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("interview_id")
    private Long interviewId; // 关联面试ID

    @TableField("sender_id")
    private Long senderId; // 发送者用户ID

    @TableField("sender_type")
    private Integer senderType; // 发送者类型：1-企业，2-求职者

    @TableField("message_type")
    private Integer messageType; // 消息类型：1-文本，2-图片，3-文件

    @TableField("content")
    private String content; // 消息内容

    @TableField("file_url")
    private String fileUrl; // 文件URL（图片/文件/简历）

    @TableField("file_name")
    private String fileName; // 原始文件名

    @TableField("is_read")
    private Integer isRead; // 是否已读：0-未读，1-已读

    @TableField("deleted_by_user")
    private Integer deletedByUser; // 求职者是否隐藏该消息(0:否 1:是)

    @TableField("deleted_by_employer")
    private Integer deletedByEmployer; // 企业用户是否隐藏该消息(0:否 1:是)

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
