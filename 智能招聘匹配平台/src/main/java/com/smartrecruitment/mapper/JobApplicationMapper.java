package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {

    /**
     * 获取用户的投递记录（带职位信息）
     */
    @Select("SELECT ja.*, " +
            "COALESCE(j.title, CONCAT('职位#', ja.job_id)) as job_title, " +
            "j.location as job_location, " +
            "j.salary_min, j.salary_max, j.experience_required, j.education_required, " +
            "u.username as employer_name " +
            "FROM job_application ja " +
            "LEFT JOIN job j ON ja.job_id = j.id " +
            "LEFT JOIN user u ON j.employer_id = u.id " +
            "WHERE ja.user_id = #{userId} " +
            "ORDER BY ja.create_time DESC")
    List<Map<String, Object>> getApplicationsWithJob(@Param("userId") Long userId);

    /**
     * 获取职位的投递记录（带简历信息）
     */
    @Select("SELECT ja.*, r.name as resume_name, r.skills as resume_skills, " +
            "r.experience as resume_experience, r.education as resume_education, " +
            "r.age as resume_age, r.expected_salary, r.ai_analysis, " +
            "u.username as applicant_name " +
            "FROM job_application ja " +
            "LEFT JOIN resume r ON ja.resume_id = r.id " +
            "LEFT JOIN user u ON ja.user_id = u.id " +
            "WHERE ja.job_id = #{jobId} " +
            "ORDER BY ja.create_time DESC")
    List<Map<String, Object>> getApplicationsWithResume(@Param("jobId") Long jobId);

    /**
     * 分页获取候选人列表（企业查看投递记录）
     */
    @Select("<script>" +
            "SELECT ja.*, j.title as job_title, j.location as job_location, " +
            "r.name as resume_name, r.skills as resume_skills, " +
            "r.experience as resume_experience, r.education as resume_education, " +
            "r.age as resume_age, r.expected_salary, r.ai_analysis " +
            "FROM job_application ja " +
            "LEFT JOIN job j ON ja.job_id = j.id " +
            "LEFT JOIN resume r ON ja.resume_id = r.id " +
            "<where>" +
            "  <if test='employerId != null'>" +
            "    j.employer_id = #{employerId}" +
            "  </if>" +
            "  <if test='jobId != null'>" +
            "    AND ja.job_id = #{jobId}" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND ja.status = #{status}" +
            "  </if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (r.name LIKE CONCAT('%', #{keyword}, '%') OR r.skills LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY ja.create_time DESC" +
            "</script>")
    IPage<Map<String, Object>> getCandidatesPage(Page<Map<String, Object>> page, @Param("employerId") Long employerId, @Param("jobId") Long jobId, @Param("status") Integer status, @Param("keyword") String keyword);
}
