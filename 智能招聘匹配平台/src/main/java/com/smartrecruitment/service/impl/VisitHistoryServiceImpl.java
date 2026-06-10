package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.UserPrivacySettings;
import com.smartrecruitment.entity.VisitHistory;
import com.smartrecruitment.mapper.VisitHistoryMapper;
import com.smartrecruitment.service.UserPrivacySettingsService;
import com.smartrecruitment.service.VisitHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 访问历史服务实现
 */
@Service
public class VisitHistoryServiceImpl extends ServiceImpl<VisitHistoryMapper, VisitHistory>
        implements VisitHistoryService {

    @Autowired
    private UserPrivacySettingsService privacySettingsService;

    @Override
    @Async
    public void recordVisit(Long visitorId, String username, String userType, String avatar,
                            Integer targetType, Long targetId, Long targetOwnerId, Boolean isAnonymous) {
        // 自己访问自己的内容不记录
        if (visitorId.equals(targetOwnerId)) {
            return;
        }

        // 确定是否匿名
        boolean anonymous;
        if (isAnonymous != null) {
            anonymous = isAnonymous;
        } else {
            UserPrivacySettings settings = privacySettingsService.getOrCreateByUserId(visitorId);
            anonymous = settings.getDefaultAnonymous() == 1;
        }

        VisitHistory record = new VisitHistory();
        record.setVisitorId(visitorId);
        record.setVisitorUsername(username);
        record.setVisitorUserType(userType);
        record.setVisitorAvatar(avatar);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setTargetOwnerId(targetOwnerId);
        record.setIsAnonymous(anonymous ? 1 : 0);
        save(record);
    }

    @Override
    public Map<String, Object> getMyVisitors(Long ownerId, Integer targetType, int page, int size) {
        List<Map<String, Object>> records;

        if (targetType != null && targetType == 1) {
            records = baseMapper.getResumeVisitors(ownerId);
        } else if (targetType != null && targetType == 2) {
            records = baseMapper.getJobVisitors(ownerId);
        } else {
            // 全部类型，合并两个查询
            List<Map<String, Object>> resumeVisitors = baseMapper.getResumeVisitors(ownerId);
            List<Map<String, Object>> jobVisitors = baseMapper.getJobVisitors(ownerId);
            resumeVisitors.addAll(jobVisitors);
            // 按时间倒序排序
            resumeVisitors.sort((a, b) -> {
                Object ta = a.get("create_time");
                Object tb = b.get("create_time");
                if (ta == null || tb == null) return 0;
                return ((Comparable) tb).compareTo(ta);
            });
            records = resumeVisitors;
        }

        // 分页
        int total = records.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageRecords = fromIndex < total
                ? records.subList(fromIndex, toIndex)
                : List.of();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Map<String, Object> getVisitStats(Long ownerId, Integer targetType) {
        LambdaQueryWrapper<VisitHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitHistory::getTargetOwnerId, ownerId);
        if (targetType != null) {
            wrapper.eq(VisitHistory::getTargetType, targetType);
        }

        // 总计
        long totalCount = count(wrapper);

        // 今日
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<VisitHistory> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(VisitHistory::getTargetOwnerId, ownerId)
                .ge(VisitHistory::getCreateTime, todayStart);
        if (targetType != null) {
            todayWrapper.eq(VisitHistory::getTargetType, targetType);
        }
        long todayCount = count(todayWrapper);

        // 本周
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime weekStartTime = LocalDateTime.of(weekStart, LocalTime.MIN);
        LambdaQueryWrapper<VisitHistory> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(VisitHistory::getTargetOwnerId, ownerId)
                .ge(VisitHistory::getCreateTime, weekStartTime);
        if (targetType != null) {
            weekWrapper.eq(VisitHistory::getTargetType, targetType);
        }
        long weekCount = count(weekWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", todayCount);
        stats.put("weekCount", weekCount);
        stats.put("totalCount", totalCount);
        return stats;
    }
}
