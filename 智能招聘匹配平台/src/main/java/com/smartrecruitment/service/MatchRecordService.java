package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.MatchRecord;

import java.util.List;
import java.util.Map;

/**
 * 匹配记录服务接口
 */
public interface MatchRecordService extends IService<MatchRecord> {

    /**
     * 创建匹配记录
     */
    MatchRecord createMatchRecord(MatchRecord matchRecord);

    /**
     * 获取简历的匹配记录（带职位信息）
     */
    List<Map<String, Object>> getMatchRecordsWithJob(Long resumeId);

    /**
     * 获取职位的匹配记录（带简历信息）
     */
    List<Map<String, Object>> getMatchRecordsWithResume(Long jobId);

    /**
     * 分页获取匹配记录
     */
    IPage<MatchRecord> getMatchRecordPage(int page, int size, Long resumeId, Long jobId);

    /**
     * 更新匹配记录状态
     */
    boolean updateMatchStatus(Long id, Integer status);

    /**
     * 为简历推荐职位
     */
    List<Map<String, Object>> recommendJobsForResume(Long resumeId, int limit);

    /**
     * 为职位推荐候选人
     */
    List<Map<String, Object>> recommendCandidatesForJob(Long jobId, int limit);

    /**
     * 统计匹配记录
     */
    long countByDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 计算简历与职位的匹配得分明细
     */
    Map<String, Object> calculateMatchDetail(com.smartrecruitment.entity.Resume resume, com.smartrecruitment.entity.Job job);
}
