package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Job;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ================================================
 * 职位服务接口
 * ================================================
 *
 * 提供职位相关的业务逻辑抽象。
 *
 * 如需扩展：
 *   - 添加 getJobsByEmployerId(Long employerId) 企业职位列表
 *   - 添加 searchJobs(keyword, filters, page) 高级搜索
 *   - 添加 updateJobStatus(Long id, Integer status) 上下架
 *   - 添加 getJobStatistics() 职位统计
 *   - 添加 getRecommendedJobs(Resume resume) 为简历推荐职位
 *   - 添加 closeExpiredJobs() 自动下架过期职位
 *   - 添加 batchCreate(List<Job> jobs) 批量发布职位
 */
public interface JobService extends IService<Job> {
    Job createJob(Job job);
    Job findById(Long id);
    boolean updateJob(Long id, Job job);
    boolean deleteJob(Long id);
    boolean updateJobStatus(Long id, Integer status);
    IPage<Job> getJobPage(int page, int size, String keyword, Long employerId, String location, String experience, String education, BigDecimal salaryMin, BigDecimal salaryMax, Integer expMin, Integer expMax, String workType);

    // 数据统计方法
    long countByCreateTime(LocalDateTime createTime);
    long countByDateRange(LocalDateTime start, LocalDateTime end);
    List<Map<String, Object>> getPopularJobs(int limit);

    // 浏览量统计
    void incrementViewCount(Long jobId);
    Long getViewCount(Long jobId);

    // 字段校验
    void validateJob(Job job);
}
