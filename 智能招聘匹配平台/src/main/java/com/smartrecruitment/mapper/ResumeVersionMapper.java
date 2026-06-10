package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.ResumeVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 简历版本 Mapper
 */
@Mapper
public interface ResumeVersionMapper extends BaseMapper<ResumeVersion> {
    
    /**
     * 查询简历的所有版本（按时间倒序）
     */
    @Select("SELECT * FROM resume_version WHERE resume_id = #{resumeId} ORDER BY create_time DESC")
    List<ResumeVersion> findByResumeId(Long resumeId);
    
    /**
     * 查询最新版本号
     */
    @Select("SELECT version_number FROM resume_version WHERE resume_id = #{resumeId} ORDER BY create_time DESC LIMIT 1")
    String findLatestVersionNumber(Long resumeId);
}
