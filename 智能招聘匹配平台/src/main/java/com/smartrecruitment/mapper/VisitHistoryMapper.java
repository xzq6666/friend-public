package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.VisitHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface VisitHistoryMapper extends BaseMapper<VisitHistory> {

    /**
     * 查询谁访问了我（简历/主页类型）
     * 匿名记录隐藏访问者信息
     */
    @Select("<script>" +
            "SELECT vh.id, vh.visitor_id, vh.visitor_user_type, vh.target_id, vh.create_time, " +
            "CASE WHEN vh.is_anonymous = 1 THEN '匿名用户' ELSE vh.visitor_username END AS visitor_username, " +
            "CASE WHEN vh.is_anonymous = 1 THEN NULL ELSE vh.visitor_avatar END AS visitor_avatar, " +
            "r.name AS resume_name " +
            "FROM visit_history vh " +
            "LEFT JOIN resume r ON vh.target_id = r.id AND vh.target_type = 1 " +
            "WHERE vh.target_owner_id = #{ownerId} AND vh.target_type = 1 " +
            "ORDER BY vh.create_time DESC" +
            "</script>")
    List<Map<String, Object>> getResumeVisitors(@Param("ownerId") Long ownerId);

    /**
     * 查询谁访问了我的职位
     * 匿名记录隐藏访问者信息
     */
    @Select("<script>" +
            "SELECT vh.id, vh.visitor_id, vh.visitor_user_type, vh.target_id, vh.create_time, " +
            "CASE WHEN vh.is_anonymous = 1 THEN '匿名用户' ELSE vh.visitor_username END AS visitor_username, " +
            "CASE WHEN vh.is_anonymous = 1 THEN NULL ELSE vh.visitor_avatar END AS visitor_avatar, " +
            "j.title AS job_title " +
            "FROM visit_history vh " +
            "LEFT JOIN job j ON vh.target_id = j.id AND vh.target_type = 2 " +
            "WHERE vh.target_owner_id = #{ownerId} AND vh.target_type = 2 " +
            "ORDER BY vh.create_time DESC" +
            "</script>")
    List<Map<String, Object>> getJobVisitors(@Param("ownerId") Long ownerId);
}
