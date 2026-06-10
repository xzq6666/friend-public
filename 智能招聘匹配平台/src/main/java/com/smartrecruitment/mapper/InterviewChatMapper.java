package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.InterviewChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface InterviewChatMapper extends BaseMapper<InterviewChat> {

    /**
     * 获取面试的聊天记录（包含发送者信息）
     */
    @Select("SELECT ic.*, u.username as sender_name, u.avatar as sender_avatar, " +
            "CASE WHEN ic.sender_id = i.employer_id THEN 'EMPLOYER' ELSE 'EMPLOYEE' END as sender_user_type " +
            "FROM interview_chat ic " +
            "LEFT JOIN user u ON ic.sender_id = u.id " +
            "LEFT JOIN interview i ON ic.interview_id = i.id " +
            "WHERE ic.interview_id = #{interviewId} " +
            "AND NOT ( (i.user_id = #{userId} AND ic.deleted_by_user = 1) OR (i.employer_id = #{userId} AND ic.deleted_by_employer = 1) ) " +
            "ORDER BY ic.create_time ASC")
    List<Map<String, Object>> getChatMessages(@Param("interviewId") Long interviewId, @Param("userId") Long userId);

    /**
     * 标记消息为已读
     */
    @Update("UPDATE interview_chat SET is_read = 1 WHERE interview_id = #{interviewId} AND sender_id != #{userId} AND is_read = 0")
    int markMessagesAsRead(@Param("interviewId") Long interviewId, @Param("userId") Long userId);

    /**
     * 获取未读消息数量
     */
    @Select("SELECT COUNT(*) FROM interview_chat WHERE interview_id = #{interviewId} AND sender_id != #{userId} AND is_read = 0")
    int getUnreadCount(@Param("interviewId") Long interviewId, @Param("userId") Long userId);
}
