package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Interview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InterviewService extends IService<Interview> {

    Interview createInterview(Long applicationId, Long jobId, Long userId, Long employerId,
                              LocalDateTime interviewTime, String interviewLocation,
                              Integer interviewType, String contactPerson,
                              String contactPhone, String notes);

    /**
     * 创建直接沟通通道（人才市场功能）
     */
    Long createDirectChat(Long resumeId, Long employerId, LocalDateTime chatTime, String notes);

    /**
     * 求职者发起与心仪职位的直接沟通
     */
    Long createDirectChatFromJob(Long jobId, Long employerId, Long userId, String title);

    /**
     * 获取用户的历史聊天列表
     */
    List<Map<String, Object>> getMyChats(Long userId);

    boolean updateInterviewStatus(Long id, Integer status);

    List<Map<String, Object>> getMyInterviews(Long userId);

    List<Map<String, Object>> getEmployerInterviews(Long employerId);
}
