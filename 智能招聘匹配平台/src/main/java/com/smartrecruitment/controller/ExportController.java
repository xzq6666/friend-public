package com.smartrecruitment.controller;

import com.smartrecruitment.service.DataExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据导出控制器
 */
@RestController
@RequestMapping("/export")
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

    @Autowired
    private DataExportService dataExportService;

    /**
     * 获取预计导出记录数
     */
    @GetMapping("/{type}/preview")
    public ResponseEntity<Map<String, Object>> getExportPreview(
            @PathVariable String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            long count = dataExportService.getExportCount(type, startDate, endDate);
            result.put("count", count);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("count", 0);
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 导出用户数据
     */
    @GetMapping("/users")
    public void exportUsers(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        try {
            dataExportService.exportUsers(response, format, fields, startDate, endDate);
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    /**
     * 导出简历数据
     */
    @GetMapping("/resumes")
    public void exportResumes(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        try {
            dataExportService.exportResumes(response, format, fields, startDate, endDate);
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    /**
     * 导出职位数据
     */
    @GetMapping("/jobs")
    public void exportJobs(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        try {
            dataExportService.exportJobs(response, format, fields, startDate, endDate);
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    /**
     * 导出匹配记录数据
     */
    @GetMapping("/match-records")
    public void exportMatchRecords(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        try {
            dataExportService.exportMatchRecords(response, format, fields, startDate, endDate);
        } catch (IOException e) {
            response.setStatus(500);
        }
    }
}
