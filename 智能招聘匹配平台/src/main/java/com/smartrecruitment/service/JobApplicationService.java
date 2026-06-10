package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.JobApplication;

import java.util.List;
import java.util.Map;

/**
 * 投递记录服务接口
 */
public interface JobApplicationService extends IService<JobApplication> {

    /**
     * 投递简历
     */
    JobApplication applyJob(Long userId, Long resumeId, Long jobId, String coverLetter);

    /**
     * 获取用户的投递记录
     */
    List<Map<String, Object>> getUserApplications(Long userId);

    /**
     * 获取职位的投递记录
     */
    List<Map<String, Object>> getJobApplications(Long jobId);

    /**
     * 分页获取投递记录
     */
    IPage<Map<String, Object>> getCandidatesPage(int page, int size, Long employerId, Long jobId, Integer status, String keyword);

    /**
     * 更新投递状态
     */
    boolean updateApplicationStatus(Long id, Integer status, String rejectReason);

    /**
     * 检查是否已投递
     */
    boolean hasApplied(Long userId, Long jobId);
}
