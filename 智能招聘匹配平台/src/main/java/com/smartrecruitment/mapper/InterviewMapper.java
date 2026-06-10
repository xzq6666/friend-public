package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.Interview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {

    /**
     * 获取用户的正式面试列表（排除直接沟通）
     * chat_type = 0 表示正式面试邀请
     */
    @Select("SELECT i.*, j.title as job_title, j.location as job_location, " +
            "u.username as employer_name, u.avatar as employer_avatar " +
            "FROM interview i " +
            "LEFT JOIN job j ON i.job_id = j.id " +
            "LEFT JOIN user u ON i.employer_id = u.id " +
            "WHERE i.user_id = #{userId} " +
            "AND (i.chat_type = 0 OR i.chat_type IS NULL) " +
            "ORDER BY i.interview_time DESC")
    List<Map<String, Object>> getInterviewsByUser(@Param("userId") Long userId);

    /**
     * 获取企业发出的正式面试列表（排除直接沟通）
     * chat_type = 0 表示正式面试邀请
     */
    @Select("SELECT i.*, j.title as job_title, u.username as applicant_name, " +
            "u.phone as applicant_phone, u.email as applicant_email, u.avatar as applicant_avatar " +
            "FROM interview i " +
            "LEFT JOIN job j ON i.job_id = j.id " +
            "LEFT JOIN user u ON i.user_id = u.id " +
            "WHERE i.employer_id = #{employerId} " +
            "AND (i.chat_type = 0 OR i.chat_type IS NULL) " +
            "ORDER BY i.interview_time DESC")
    List<Map<String, Object>> getInterviewsByEmployer(@Param("employerId") Long employerId);

    /**
     * 获取用户的直接沟通（聊天）列表（排除正式面试）
     * chat_type = 1 表示直接沟通
     */
    @Select("<script>" +
            "SELECT i.id, i.job_id, i.user_id, i.employer_id, i.status, i.create_time, i.update_time, " +
            "ic.content as last_message, ic.sender_id as last_sender_id, ic.create_time as last_message_time, " +
            "uc.unread_count, " +
            "j.title as job_title, " +
            "CASE " +
            "  WHEN i.employer_id = #{userId} THEN u2.username " +
            "  ELSE u1.username " +
            "END as chat_partner_name, " +
            "CASE " +
            "  WHEN i.employer_id = #{userId} THEN u2.avatar " +
            "  ELSE u1.avatar " +
            "END as chat_partner_avatar " +
            "FROM interview i " +
            "LEFT JOIN job j ON i.job_id = j.id " +
            "LEFT JOIN user u1 ON i.employer_id = u1.id " +
            "LEFT JOIN user u2 ON i.user_id = u2.id " +
            "LEFT JOIN (" +
            "  SELECT interview_id, content, sender_id, create_time " +
            "  FROM interview_chat " +
            "  WHERE id IN (" +
            "    SELECT MAX(id) FROM interview_chat GROUP BY interview_id" +
            "  )" +
            ") ic ON i.id = ic.interview_id " +
            "LEFT JOIN (" +
            "  SELECT interview_id, COUNT(*) as unread_count " +
            "  FROM interview_chat " +
            "  WHERE is_read = 0 AND sender_id != #{userId} GROUP BY interview_id" +
            ") uc ON i.id = uc.interview_id " +
            "WHERE (i.user_id = #{userId} OR i.employer_id = #{userId}) " +
            "AND (i.chat_type = 1 OR i.interview_location = '在线沟通') " +
            "AND NOT ( (i.user_id = #{userId} AND i.deleted_by_user = 1) OR (i.employer_id = #{userId} AND i.deleted_by_employer = 1) ) " +
            "ORDER BY COALESCE(ic.create_time, i.create_time) DESC" +
            "</script>")
    List<Map<String, Object>> getMyChats(@Param("userId") Long userId);
}
