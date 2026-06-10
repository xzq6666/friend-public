 package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.entity.CompanyInfo;
import com.smartrecruitment.mapper.CompanyInfoMapper;
import com.smartrecruitment.mapper.JobApplicationMapper;
import com.smartrecruitment.mapper.JobMapper;
import com.smartrecruitment.service.JobKeywordService;
import com.smartrecruitment.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);
    private static final String JOB_CACHE_PREFIX = "job:detail:";
    private static final String JOB_VIEW_PREFIX = "job:view:";
    private static final int JOB_CACHE_HOURS = 2;

    @Autowired
    private JobKeywordService jobKeywordService;

    @Autowired
    private JobApplicationMapper jobApplicationMapper;

    @Autowired
    private CompanyInfoMapper companyInfoMapper;

    @Autowired
    private com.smartrecruitment.service.UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Job createJob(Job job) {
        validateJob(job);
        save(job);
        if (job.getDescription() != null || job.getRequirements() != null) {
            String text = (job.getDescription() != null ? job.getDescription() : "") + " " +
                          (job.getRequirements() != null ? job.getRequirements() : "");
            jobKeywordService.linkKeywordsToJob(job.getId(), text, "SYSTEM");
        }
        return job;
    }

    @Override
    public Job findById(Long id) {
        String cacheKey = JOB_CACHE_PREFIX + id;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (Job) cached;
            }
        } catch (Exception e) {
            log.warn("读取职位缓存失败: {}", e.getMessage());
        }

        Job job = getById(id);
        if (job != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, job, JOB_CACHE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("写入职位缓存失败: {}", e.getMessage());
            }
        }
        return job;
    }

    @Override
    public boolean updateJob(Long id, Job job) {
        // 对于更新操作，先加载已有记录，合并后再验证
        Job existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("职位不存在");
        }
        // 合并非空字段
        if (job.getTitle() != null) existing.setTitle(job.getTitle());
        if (job.getDescription() != null) existing.setDescription(job.getDescription());
        if (job.getRequirements() != null) existing.setRequirements(job.getRequirements());
        if (job.getLocation() != null) existing.setLocation(job.getLocation());
        if (job.getSalaryMin() != null) existing.setSalaryMin(job.getSalaryMin());
        if (job.getSalaryMax() != null) existing.setSalaryMax(job.getSalaryMax());
        if (job.getExperienceRequired() != null) existing.setExperienceRequired(job.getExperienceRequired());
        if (job.getEducationRequired() != null) existing.setEducationRequired(job.getEducationRequired());
        if (job.getWorkType() != null) existing.setWorkType(job.getWorkType());
        if (job.getCategoryId() != null) existing.setCategoryId(job.getCategoryId());
        if (job.getStatus() != null) existing.setStatus(job.getStatus());
        validateJob(existing);
        boolean success = updateById(existing);
        if (success) {
            evictJobCache(id);
            evictMatchRecommendCache(id);
            if (existing.getDescription() != null || existing.getRequirements() != null) {
                String text = (existing.getDescription() != null ? existing.getDescription() : "") + " " +
                              (existing.getRequirements() != null ? existing.getRequirements() : "");
                jobKeywordService.linkKeywordsToJob(id, text, "SYSTEM");
            }
        }
        return success;
    }

    @Override
    public boolean deleteJob(Long id) {
        // 检查是否有投递记录
        long appCount = jobApplicationMapper.selectCount(
            new LambdaQueryWrapper<JobApplication>().eq(JobApplication::getJobId, id)
        );
        if (appCount > 0) {
            throw new RuntimeException("该职位已有投递记录，不能直接删除。请先下线职位。");
        }
        boolean success = removeById(id);
        if (success) {
            evictJobCache(id);
            evictMatchRecommendCache(id);
        }
        return success;
    }

    @Override
    public boolean updateJobStatus(Long id, Integer status) {
        Job job = getById(id);
        if (job != null) {
            job.setStatus(status);
            boolean success = updateById(job);
            if (success) {
                evictJobCache(id);
                evictMatchRecommendCache(id);
            }
            return success;
        }
        return false;
    }

    @Override
    public IPage<Job> getJobPage(int page, int size, String keyword, Long employerId, String location, String experience, String education, BigDecimal salaryMin, BigDecimal salaryMax, Integer expMin, Integer expMax, String workType) {
        IPage<Job> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            // 先按公司名模糊查询，拿到匹配的企业用户ID
            LambdaQueryWrapper<CompanyInfo> companyWrapper = new LambdaQueryWrapper<>();
            companyWrapper.like(CompanyInfo::getCompanyName, keyword)
                    .select(CompanyInfo::getUserId);
            List<CompanyInfo> matchedCompanies = companyInfoMapper.selectList(companyWrapper);
            List<Long> matchedEmployerIds = matchedCompanies.stream()
                    .map(CompanyInfo::getUserId)
                    .collect(Collectors.toList());

            wrapper.and(w -> {
                w.like(Job::getTitle, keyword)
                        .or()
                        .like(Job::getDescription, keyword);
                if (!matchedEmployerIds.isEmpty()) {
                    w.or().in(Job::getEmployerId, matchedEmployerIds);
                }
            });
        }

        if (employerId != null) {
            wrapper.eq(Job::getEmployerId, employerId);
        }

        if (location != null && !location.isEmpty()) {
            wrapper.like(Job::getLocation, location);
        }

        if (experience != null && !experience.isEmpty()) {
            wrapper.and(w -> w.like(Job::getExperienceRequired, experience)
                    .or()
                    .like(Job::getExperienceRequired, experience.split("-")[0]));
        }

        if (education != null && !education.isEmpty()) {
            wrapper.eq(Job::getEducationRequired, education);
        }

        // 薪资范围筛选
        if (salaryMin != null) {
            wrapper.ge(Job::getSalaryMax, salaryMin);
        }
        if (salaryMax != null) {
            wrapper.le(Job::getSalaryMin, salaryMax);
        }

        // 经验年限筛选（根据 experience_required 字段模糊匹配）
        if (expMin != null && expMax != null) {
            wrapper.and(w -> w.like(Job::getExperienceRequired, expMin + "年")
                    .or().like(Job::getExperienceRequired, expMax + "年")
                    .or().like(Job::getExperienceRequired, "不限")
                    .or().like(Job::getExperienceRequired, "应届"));
        } else if (expMin != null) {
            wrapper.and(w -> w.like(Job::getExperienceRequired, expMin + "年")
                    .or().like(Job::getExperienceRequired, "不限")
                    .or().like(Job::getExperienceRequired, "应届"));
        } else if (expMax != null) {
            wrapper.and(w -> w.like(Job::getExperienceRequired, expMax + "年")
                    .or().like(Job::getExperienceRequired, "不限")
                    .or().like(Job::getExperienceRequired, "应届"));
        }

        // 工作类型筛选
        if (workType != null && !workType.isEmpty()) {
            wrapper.eq(Job::getWorkType, workType);
        }

        wrapper.orderByDesc(Job::getCreateTime);
        return page(pageResult, wrapper);
    }

    @Override
    public long countByCreateTime(LocalDateTime createTime) {
        LocalDateTime startOfDay = createTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return lambdaQuery().ge(Job::getCreateTime, startOfDay).lt(Job::getCreateTime, endOfDay).count();
    }

    @Override
    public long countByDateRange(LocalDateTime start, LocalDateTime end) {
        return lambdaQuery().ge(Job::getCreateTime, start).lt(Job::getCreateTime, end).count();
    }

    @Override
    public List<Map<String, Object>> getPopularJobs(int limit) {
        // 先查询有投递记录的职位ID，按投递数量排序
        List<Job> jobs = lambdaQuery()
                .eq(Job::getStatus, 1)
                .last("limit " + limit * 3) // 多取一些，后续按热度排序截取
                .list();

        List<Map<String, Object>> result = jobs.stream()
                .map(job -> {
                    Map<String, Object> jobData = new HashMap<>();
                    jobData.put("id", job.getId());
                    jobData.put("title", job.getTitle());
                    // 查询公司名称而非使用employerId
                    try {
                        com.smartrecruitment.entity.User employer = userService.getById(job.getEmployerId());
                        jobData.put("company", employer != null ? employer.getUsername() : "未知企业");
                    } catch (Exception e) {
                        jobData.put("company", "未知企业");
                    }
                    Long appCount = jobApplicationMapper.selectCount(
                        new LambdaQueryWrapper<JobApplication>().eq(JobApplication::getJobId, job.getId())
                    );
                    jobData.put("matchCount", appCount != null ? appCount : 0);
                    jobData.put("viewCount", getViewCount(job.getId()));
                    // 综合热度 = 投递数*2 + 浏览量
                    jobData.put("popularity", (appCount != null ? appCount : 0) * 2 + getViewCount(job.getId()));
                    return jobData;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("popularity"), (Long) a.get("popularity")))
                .limit(limit)
                .collect(Collectors.toList());

        return result;
    }

    // ==================== 浏览量统计 ====================

    @Override
    public void incrementViewCount(Long jobId) {
        String key = JOB_VIEW_PREFIX + jobId;
        try {
            redisTemplate.opsForValue().increment(key);
            // 设置过期时间，定期同步到数据库
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("增加浏览量失败: {}", e.getMessage());
        }
    }

    @Override
    public Long getViewCount(Long jobId) {
        String key = JOB_VIEW_PREFIX + jobId;
        try {
            Object count = redisTemplate.opsForValue().get(key);
            if (count != null) {
                return Long.parseLong(count.toString());
            }
        } catch (Exception e) {
            log.warn("读取浏览量失败: {}", e.getMessage());
        }
        return 0L;
    }

    // ==================== 字段校验 ====================

    @Override
    public void validateJob(Job job) {
        List<String> errors = new ArrayList<>();

        if (job.getTitle() == null || job.getTitle().trim().isEmpty()) {
            errors.add("职位名称不能为空");
        } else if (job.getTitle().length() > 100) {
            errors.add("职位名称不能超过100个字符");
        }

        if (job.getDescription() == null || job.getDescription().trim().isEmpty()) {
            errors.add("职位描述不能为空");
        }

        if (job.getRequirements() == null || job.getRequirements().trim().isEmpty()) {
            errors.add("职位要求不能为空");
        }

        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            if (job.getSalaryMin().compareTo(job.getSalaryMax()) > 0) {
                errors.add("最低薪资不能高于最高薪资");
            }
            if (job.getSalaryMin().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("薪资不能为负数");
            }
        }

        if (job.getEmployerId() == null) {
            errors.add("必须指定发布者");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    // ==================== 缓存管理 ====================

    private void evictJobCache(Long jobId) {
        try {
            redisTemplate.delete(JOB_CACHE_PREFIX + jobId);
        } catch (Exception e) {
            log.warn("清除职位缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除职位匹配推荐缓存（职位变更时调用）
     */
    private void evictMatchRecommendCache(Long jobId) {
        try {
            redisTemplate.delete("match:recommend:job:" + jobId);
            log.info("已清除职位匹配推荐缓存: jobId={}", jobId);
        } catch (Exception e) {
            log.warn("清除职位匹配推荐缓存失败: {}", e.getMessage());
        }
    }
}
