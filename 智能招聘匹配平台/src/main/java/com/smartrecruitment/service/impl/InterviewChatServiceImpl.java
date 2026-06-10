package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Interview;
import com.smartrecruitment.entity.InterviewChat;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.mapper.InterviewChatMapper;
import com.smartrecruitment.mapper.InterviewMapper;
import com.smartrecruitment.service.InterviewChatService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InterviewChatServiceImpl extends ServiceImpl<InterviewChatMapper, InterviewChat> implements InterviewChatService {

    @Autowired
    private InterviewChatMapper chatMapper;

    @Autowired
    private InterviewMapper interviewMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Override
    public List<Map<String, Object>> getChatMessages(Long interviewId, Long userId) {
        return chatMapper.getChatMessages(interviewId, userId);
    }

    @Override
    public InterviewChat sendMessage(Long interviewId, Long senderId, Integer senderType, String content, Integer messageType) {
        // 发送消息时，重置对方的删除标记（让对方能重新看到这个聊天）
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview != null) {
            if (interview.getUserId().equals(senderId)) {
                // 求职者发送，重置企业的删除标记
                interview.setDeletedByEmployer(0);
            } else {
                // 企业发送，重置求职者的删除标记
                interview.setDeletedByUser(0);
            }
            interviewMapper.updateById(interview);
        }

        InterviewChat chat = new InterviewChat();
        chat.setInterviewId(interviewId);
        chat.setSenderId(senderId);
        chat.setSenderType(senderType);
        chat.setContent(content);
        chat.setMessageType(messageType != null ? messageType : 1);
        chat.setIsRead(0);
        save(chat);

        // 创建通知给接收方
        try {
            createChatNotification(interview, senderId, content);
        } catch (Exception e) {
            // 通知创建失败不影响消息发送
        }

        return chat;
    }

    /**
     * 创建聊天消息通知
     */
    private void createChatNotification(Interview interview, Long senderId, String content) {
        if (interview == null) return;

        // 确定接收者ID
        Long receiverId = interview.getUserId().equals(senderId)
                ? interview.getEmployerId()
                : interview.getUserId();

        // 获取发送者信息
        User sender = userService.getById(senderId);
        String senderName = sender != null ? sender.getUsername() : "用户";

        // 截取消息内容作为通知描述
        String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;

        notificationService.send(
                receiverId,
                senderName + " 发来新消息",
                preview + "\ninterviewId:" + interview.getId(),
                "chat"
        );
    }

    @Override
    public InterviewChat sendFileMessage(Long interviewId, Long senderId, Integer senderType, String content, Integer messageType, String fileUrl, String fileName) {
        // 发送文件时，重置对方的删除标记
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview != null) {
            if (interview.getUserId().equals(senderId)) {
                interview.setDeletedByEmployer(0);
            } else {
                interview.setDeletedByUser(0);
            }
            interviewMapper.updateById(interview);
        }

        InterviewChat chat = new InterviewChat();
        chat.setInterviewId(interviewId);
        chat.setSenderId(senderId);
        chat.setSenderType(senderType);
        chat.setContent(content);
        chat.setMessageType(messageType != null ? messageType : 2);
        chat.setFileUrl(fileUrl);
        chat.setFileName(fileName);
        chat.setIsRead(0);
        save(chat);
        return chat;
    }

    @Override
    public boolean markMessagesAsRead(Long interviewId, Long userId) {
        return chatMapper.markMessagesAsRead(interviewId, userId) > 0;
    }

    @Override
    public int getUnreadCount(Long interviewId, Long userId) {
        return chatMapper.getUnreadCount(interviewId, userId);
    }

    @Override
    public void deleteChatMessages(Long interviewId) {
        chatMapper.deleteByMap(Map.of("interview_id", interviewId));
    }
}
