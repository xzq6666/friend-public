package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.MatchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ================================================
 * 匹配记录 Mapper - MyBatis-Plus 基础 CRUD
 * ================================================
 *
 * 提供 MatchRecord 实体对 match_record 表的自动 CRUD 操作。
 *
 * 如需扩展：
 *   - 添加按简历ID或职位ID查询匹配记录
 *   - 添加按匹配度分数范围筛选
 *   - 添加统计各状态记录数量的聚合查询
 *   - 添加分页查询匹配记录列表（含关联简历和职位信息）
 */
@Mapper
public interface MatchRecordMapper extends BaseMapper<MatchRecord> {

    long countByCreateTime(@Param("createTime") LocalDateTime createTime);

    long countByDateRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    List<Map<String, Object>> getMatchRecordsWithJob(@Param("resumeId") Long resumeId);

    List<Map<String, Object>> getMatchRecordsWithResume(@Param("jobId") Long jobId);
}
