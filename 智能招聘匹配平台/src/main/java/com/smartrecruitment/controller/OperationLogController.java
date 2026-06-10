package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartrecruitment.entity.OperationLog;
import com.smartrecruitment.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/operation-log")
@CrossOrigin
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    public ResponseEntity<?> getLogPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation) {
        try {
            IPage<OperationLog> logPage = operationLogService.getLogPage(page, size, username, module, operation);
            return ResponseEntity.ok(logPage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteLog(@PathVariable Long id) {
        try {
            boolean success = operationLogService.removeById(id);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "删除成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 清空日志
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearLogs() {
        try {
            operationLogService.remove(null);
            return ResponseEntity.ok(Map.of("message", "日志已清空"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
