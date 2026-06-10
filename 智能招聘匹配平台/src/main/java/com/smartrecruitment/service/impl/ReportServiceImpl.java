package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Report;
import com.smartrecruitment.mapper.ReportMapper;
import com.smartrecruitment.service.ReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {
    
    @Override
    public Report submitReport(Report report) {
        report.setStatus(0); // 待处理
        save(report);
        return getById(report.getId());
    }
    
    @Override
    public boolean handleReport(Long reportId, Integer status, String handleResult, Long handlerId) {
        Report report = getById(reportId);
        if (report == null) {
            return false;
        }
        
        report.setStatus(status);
        report.setHandleResult(handleResult);
        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        
        return updateById(report);
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", count());
        stats.put("pending", lambdaQuery().eq(Report::getStatus, 0).count());
        stats.put("processing", lambdaQuery().eq(Report::getStatus, 1).count());
        stats.put("handled", lambdaQuery().eq(Report::getStatus, 2).count());
        stats.put("rejected", lambdaQuery().eq(Report::getStatus, 3).count());
        return stats;
    }
}
