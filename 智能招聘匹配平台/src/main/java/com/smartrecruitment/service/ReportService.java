package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Report;

import java.util.Map;

public interface ReportService extends IService<Report> {
    /**
     * 提交举报
     */
    Report submitReport(Report report);

    /**
     * 处理举报（管理员）
     */
    boolean handleReport(Long reportId, Integer status, String handleResult, Long handlerId);

    /**
     * 获取举报统计数据
     */
    Map<String, Object> getStats();
}
