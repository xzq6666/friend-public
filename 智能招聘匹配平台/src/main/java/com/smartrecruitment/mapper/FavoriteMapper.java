package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 获取收藏的职位
     * folderId 为 null 或 0 表示全部收藏（不过滤文件夹）
     * folderId > 0 表示只显示该文件夹内的收藏
     */
    @Select("<script>" +
            "SELECT f.*, j.title as job_title, j.location as job_location, " +
            "j.salary_min, j.salary_max, u.username as employer_name " +
            "FROM favorite f " +
            "LEFT JOIN job j ON f.target_id = j.id AND f.target_type = 1 " +
            "LEFT JOIN user u ON j.employer_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.target_type = 1 " +
            "<if test='folderId != null and folderId > 0'> AND f.folder_id = #{folderId} </if>" +
            "ORDER BY f.create_time DESC" +
            "</script>")
    List<Map<String, Object>> getFavoriteJobs(@Param("userId") Long userId, @Param("folderId") Long folderId);

    /**
     * 获取收藏的简历
     * folderId 为 null 或 0 表示全部收藏（不过滤文件夹）
     * folderId > 0 表示只显示该文件夹内的收藏
     */
    @Select("<script>" +
            "SELECT f.*, r.name as resume_name, r.skills as resume_skills, " +
            "r.experience as resume_experience, r.education as resume_education, " +
            "u.username as applicant_name " +
            "FROM favorite f " +
            "LEFT JOIN resume r ON f.target_id = r.id AND f.target_type = 2 " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.target_type = 2 " +
            "<if test='folderId != null and folderId > 0'> AND f.folder_id = #{folderId} </if>" +
            "ORDER BY f.create_time DESC" +
            "</script>")
    List<Map<String, Object>> getFavoriteResumes(@Param("userId") Long userId, @Param("folderId") Long folderId);
}
