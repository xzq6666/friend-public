package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.mapper.JobApplicationMapper;
import com.smartrecruitment.mapper.JobMapper;
import com.smartrecruitment.service.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 投递记录服务实现 - 增强版
 * 增加状态流转校验
 */
@Service
public class JobApplicationServiceImpl extends ServiceImpl<JobApplicationMapper, JobApplication> implements JobApplicationService {

    @Autowired
    private JobMapper jobMapper;

    /**
     * 合法状态流转规则：
     * 0(待处理) -> 1(已查看), 2(邀请面试), 4(已拒绝)
     * 1(已查看) -> 2(邀请面试), 4(已拒绝)
     * 2(邀请面试) -> 3(已录用), 4(已拒绝)
     * 3(已录用) -> 终态，不可再流转
     * 4(已拒绝) -> 终态，不可再流转
     */
    private static final Map<Integer, Set<Integer>> VALID_TRANSITIONS = new HashMap<>();
    static {
        VALID_TRANSITIONS.put(0, Set.of(1, 2, 4));
        VALID_TRANSITIONS.put(1, Set.of(2, 4));
        VALID_TRANSITIONS.put(2, Set.of(3, 4));
        VALID_TRANSITIONS.put(3, Set.of()); // 终态
        VALID_TRANSITIONS.put(4, Set.of()); // 终态
    }

    @Override
    public JobApplication applyJob(Long userId, Long resumeId, Long jobId, String coverLetter) {
        if (hasApplied(userId, jobId)) {
            throw new RuntimeException("您已经投递过该职位");
        }

        // 校验职位存在且在线
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("职位不存在");
        }
        if (job.getStatus() != null && job.getStatus() != 1) {
            throw new RuntimeException("该职位已下线，暂不接受投递");
        }
        // 不能投递自己发布的职位
        if (job.getEmployerId() != null && job.getEmployerId().equals(userId)) {
            throw new RuntimeException("不能投递自己发布的职位");
        }

        JobApplication application = new JobApplication();
        application.setUserId(userId);
        application.setResumeId(resumeId);
        application.setJobId(jobId);
        application.setCoverLetter(coverLetter);
        application.setStatus(0);

        save(application);
        return application;
    }

    @Override
    public List<Map<String, Object>> getUserApplications(Long userId) {
        return baseMapper.getApplicationsWithJob(userId);
    }

    @Override
    public List<Map<String, Object>> getJobApplications(Long jobId) {
        return baseMapper.getApplicationsWithResume(jobId);
    }

    @Override
    public IPage<Map<String, Object>> getCandidatesPage(int page, int size, Long employerId, Long jobId, Integer status, String keyword) {
        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        return baseMapper.getCandidatesPage(pageResult, employerId, jobId, status, keyword);
    }

    @Override
    public boolean updateApplicationStatus(Long id, Integer status, String rejectReason) {
        JobApplication application = getById(id);
        if (application == null) {
            throw new RuntimeException("投递记录不存在");
        }

        // 状态流转校验
        Integer currentStatus = application.getStatus();
        Set<Integer> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(status)) {
            throw new RuntimeException(String.format(
                "状态流转不合法：当前状态[%s] -> 目标状态[%s]",
                getStatusText(currentStatus), getStatusText(status)
            ));
        }

        // 拒绝时必须填写原因
        if (status == 4 && (rejectReason == null || rejectReason.trim().isEmpty())) {
            throw new RuntimeException("拒绝时必须填写拒绝原因");
        }

        application.setStatus(status);
        if (rejectReason != null) {
            application.setRejectReason(rejectReason);
        }
        return updateById(application);
    }

    @Override
    public boolean hasApplied(Long userId, Long jobId) {
        return lambdaQuery()
                .eq(JobApplication::getUserId, userId)
                .eq(JobApplication::getJobId, jobId)
                .ne(JobApplication::getStatus, 4) // 排除已拒绝的记录，允许重新投递
                .count() > 0;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已查看";
            case 2 -> "邀请面试";
            case 3 -> "已录用";
            case 4 -> "已拒绝";
            default -> "未知";
        };
    }
}
