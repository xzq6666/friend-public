package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Interview;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.mapper.InterviewMapper;
import com.smartrecruitment.service.InterviewService;
import com.smartrecruitment.service.JobApplicationService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class InterviewServiceImpl extends ServiceImpl<InterviewMapper, Interview> implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);

    // 面试时间冲突检测窗口（前后30分钟）
    private static final int CONFLICT_MINUTES = 30;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public Interview createInterview(Long applicationId, Long jobId, Long userId, Long employerId,
                                     LocalDateTime interviewTime, String interviewLocation,
                                     Integer interviewType, String contactPerson,
                                     String contactPhone, String notes) {
        // 校验面试时间不能是过去时间
        if (interviewTime != null && interviewTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("面试时间不能早于当前时间");
        }

        // 面试时间冲突检测
        if (interviewTime != null) {
            checkTimeConflict(userId, employerId, interviewTime);
        }

        // 校验投递记录存在且状态合法（仅当提供了 applicationId 时校验）
        if (applicationId != null) {
            JobApplication application = jobApplicationService.getById(applicationId);
            if (application == null) {
                throw new RuntimeException("投递记录不存在");
            }
            if (application.getStatus() != 1 && application.getStatus() != 2) {
                throw new RuntimeException("该投递当前状态不能安排面试");
            }
        }

        Interview interview = new Interview();
        interview.setApplicationId(applicationId);
        interview.setJobId(jobId);
        interview.setUserId(userId);
        interview.setEmployerId(employerId);
        interview.setInterviewTime(interviewTime);
        interview.setInterviewLocation(interviewLocation);
        interview.setInterviewType(interviewType);
        interview.setContactPerson(contactPerson);
        interview.setContactPhone(contactPhone);
        interview.setNotes(notes);
        interview.setStatus(0);
        save(interview);

        // 如果存在投递记录，同步更新投递状态为"邀请面试"
        if (applicationId != null) {
            jobApplicationService.updateApplicationStatus(applicationId, 2, null);
        }

        // 创建通知给求职者
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStr = interviewTime != null ? interviewTime.format(formatter) : "待定";
            notificationService.send(
                    userId,
                    "新的面试邀请",
                    String.format("您收到一个面试邀请，面试时间：%s，地点：%s", timeStr, interviewLocation != null ? interviewLocation : "待定"),
                    "interview"
            );
        } catch (Exception e) {
            log.warn("创建面试通知失败: {}", e.getMessage());
        }

        return interview;
    }

    /**
     * 面试时间冲突检测
     * 检查同一面试官或同一候选人在面试时间前后30分钟内是否已有其他面试安排
     */
    private void checkTimeConflict(Long userId, Long employerId, LocalDateTime interviewTime) {
        LocalDateTime windowStart = interviewTime.minusMinutes(CONFLICT_MINUTES);
        LocalDateTime windowEnd = interviewTime.plusMinutes(CONFLICT_MINUTES);

        // 检查面试官时间冲突（包括待确认和已确认的面试）
        List<Interview> employerInterviews = lambdaQuery()
                .eq(Interview::getEmployerId, employerId)
                .in(Interview::getStatus, 0, 1) // 待确认和已确认的面试
                .ge(Interview::getInterviewTime, windowStart)
                .le(Interview::getInterviewTime, windowEnd)
                .list();

        if (!employerInterviews.isEmpty()) {
            throw new RuntimeException(String.format(
                "面试官在 %s 前后%d分钟内已有其他面试安排，请选择其他时间",
                interviewTime, CONFLICT_MINUTES
            ));
        }

        // 检查候选人时间冲突（包括待确认和已确认的面试）
        List<Interview> userInterviews = lambdaQuery()
                .eq(Interview::getUserId, userId)
                .in(Interview::getStatus, 0, 1)
                .ge(Interview::getInterviewTime, windowStart)
                .le(Interview::getInterviewTime, windowEnd)
                .list();

        if (!userInterviews.isEmpty()) {
            throw new RuntimeException(String.format(
                "候选人在 %s 前后%d分钟内已有其他面试安排，请选择其他时间",
                interviewTime, CONFLICT_MINUTES
            ));
        }
    }

    @Override
    public boolean updateInterviewStatus(Long id, Integer status) {
        Interview interview = getById(id);
        if (interview == null) {
            return false;
        }

        // 状态流转校验
        int currentStatus = interview.getStatus();
        // 0(待确认) -> 1(已确认), 2(已取消)
        // 1(已确认) -> 3(已完成), 2(已取消)
        // 2(已取消) -> 终态
        // 3(已完成) -> 终态
        boolean valid = false;
        if (currentStatus == 0 && (status == 1 || status == 2)) valid = true;
        else if (currentStatus == 1 && (status == 3 || status == 2)) valid = true;

        if (!valid) {
            throw new RuntimeException(String.format(
                "面试状态流转不合法：当前[%s] -> 目标[%s]",
                getStatusText(currentStatus), getStatusText(status)
            ));
        }

        interview.setStatus(status);

        // 如果面试取消，同步更新投递状态回"已查看"
        if (status == 2) {
            updateById(interview);
            if (interview.getApplicationId() != null) {
                try {
                    // 直接更新投递状态，绕过状态流转校验（因为投递当前状态是"邀请面试"=2，需要回退到"已查看"=1）
                    JobApplication app = jobApplicationService.getById(interview.getApplicationId());
                    if (app != null && app.getStatus() == 2) {
                        app.setStatus(1);
                        jobApplicationService.updateById(app);
                    }
                } catch (Exception e) {
                    log.warn("同步更新投递状态失败: {}", e.getMessage());
                }
            }
            return true;
        }

        // 如果面试完成，同步更新投递状态
        if (status == 3) {
            updateById(interview);
            return true;
        }

        return updateById(interview);
    }

    @Override
    public List<Map<String, Object>> getMyInterviews(Long userId) {
        return baseMapper.getInterviewsByUser(userId);
    }

    @Override
    public List<Map<String, Object>> getEmployerInterviews(Long employerId) {
        return baseMapper.getInterviewsByEmployer(employerId);
    }

    @Override
    public Long createDirectChat(Long resumeId, Long employerId, LocalDateTime chatTime, String notes) {
        // 校验简历是否存在
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        // 创建聊天通道记录（chatType=1 表示直接沟通，不算面试）
        Interview interview = new Interview();
        interview.setResumeId(resumeId);
        interview.setUserId(resume.getUserId());
        interview.setEmployerId(employerId);
        interview.setInterviewTime(chatTime);
        interview.setInterviewLocation("在线沟通");
        interview.setInterviewType(2); // 视频/在线
        interview.setContactPerson("企业HR");
        interview.setNotes(notes != null ? notes : "人才市场直接沟通");
        interview.setChatType(1); // 标记为直接沟通，不算面试
        interview.setStatus(1); // 直接确认为已确认状态
        save(interview);

        return interview.getId();
    }

    @Override
    public Long createDirectChatFromJob(Long jobId, Long employerId, Long userId, String title) {
        // 创建聊天通道记录（chatType=1 表示直接沟通，不算面试）
        Interview interview = new Interview();
        interview.setJobId(jobId);
        interview.setUserId(userId);
        interview.setEmployerId(employerId);
        interview.setInterviewTime(LocalDateTime.now());
        interview.setInterviewLocation("在线沟通");
        interview.setInterviewType(2); // 视频/在线
        interview.setContactPerson("企业HR");
        interview.setNotes(title != null ? title : "在线沟通");
        interview.setChatType(1); // 标记为直接沟通，不算面试
        interview.setStatus(1); // 直接确认为已确认状态
        save(interview);

        return interview.getId();
    }

    @Override
    public List<Map<String, Object>> getMyChats(Long userId) {
        return baseMapper.getMyChats(userId);
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 0 -> "待确认";
            case 1 -> "已确认";
            case 2 -> "已取消";
            case 3 -> "已完成";
            default -> "未知";
        };
    }
}
