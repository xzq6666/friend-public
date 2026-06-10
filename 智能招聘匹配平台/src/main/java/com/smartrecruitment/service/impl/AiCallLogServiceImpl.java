package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.AICallLog;
import com.smartrecruitment.mapper.AiCallLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiCallLogServiceImpl extends ServiceImpl<AiCallLogMapper, AICallLog> {

    /**
     * 异步记录AI调用日志
     */
    @Async
    public void logCall(String callType, Long userId, Long resumeId, Long jobId,
                        String model, int promptLength, int responseLength,
                        Integer promptTokens, Integer completionTokens, Integer totalTokens,
                        long elapsedMs, String status, String errorMessage, String responseSummary) {
        try {
            AICallLog log = new AICallLog();
            log.setCallType(callType);
            log.setUserId(userId);
            log.setResumeId(resumeId);
            log.setJobId(jobId);
            log.setModel(model);
            log.setPromptLength(promptLength);
            log.setResponseLength(responseLength);
            log.setPromptTokens(promptTokens);
            log.setCompletionTokens(completionTokens);
            log.setTotalTokens(totalTokens);
            log.setElapsedMs(elapsedMs);
            log.setStatus(status);
            log.setErrorMessage(errorMessage);
            if (responseSummary != null && responseSummary.length() > 200) {
                responseSummary = responseSummary.substring(0, 200);
            }
            log.setResponseSummary(responseSummary);
            save(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
        }
    }

    /**
     * 获取调用统计
     */
    public Map<String, Object> getStatistics(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<AICallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(AICallLog::getCreateTime, start).lt(AICallLog::getCreateTime, end);

        List<AICallLog> logs = list(wrapper);

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalCalls", logs.size());
        stats.put("successCount", logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count());
        stats.put("failedCount", logs.stream().filter(l -> "FAILED".equals(l.getStatus())).count());
        stats.put("avgElapsedMs", logs.stream().mapToLong(AICallLog::getElapsedMs).average().orElse(0));
        stats.put("totalTokens", logs.stream().mapToInt(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum());
        stats.put("avgTokens", logs.stream().mapToInt(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).average().orElse(0));

        // 按类型分组
        Map<String, Long> byType = logs.stream()
                .collect(Collectors.groupingBy(AICallLog::getCallType, Collectors.counting()));
        stats.put("byType", byType);

        return stats;
    }
}
