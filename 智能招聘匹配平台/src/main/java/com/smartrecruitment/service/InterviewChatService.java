package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.InterviewChat;

import java.util.List;
import java.util.Map;

public interface InterviewChatService extends IService<InterviewChat> {

    /**
     * 获取面试的聊天记录
     */
    List<Map<String, Object>> getChatMessages(Long interviewId, Long userId);

    /**
     * 发送消息
     */
    InterviewChat sendMessage(Long interviewId, Long senderId, Integer senderType, String content, Integer messageType);

    /**
     * 发送文件消息
     */
    InterviewChat sendFileMessage(Long interviewId, Long senderId, Integer senderType, String content, Integer messageType, String fileUrl, String fileName);

    /**
     * 标记消息为已读
     */
    boolean markMessagesAsRead(Long interviewId, Long userId);

    /**
     * 获取未读消息数量
     */
    int getUnreadCount(Long interviewId, Long userId);

    /**
     * 删除指定面试的所有聊天消息
     */
    void deleteChatMessages(Long interviewId);
}
