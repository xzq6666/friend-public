package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.OperationLog;
import com.smartrecruitment.mapper.OperationLogMapper;
import com.smartrecruitment.service.OperationLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    @Async
    public void log(Long userId, String username, String operation, String module, String description, String ipAddress, Integer status) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setModule(module);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setStatus(status);
        save(log);
    }

    @Override
    public IPage<OperationLog> getLogPage(int page, int size, String username, String module, String operation) {
        IPage<OperationLog> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.isEmpty()) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (operation != null && !operation.isEmpty()) {
            wrapper.eq(OperationLog::getOperation, operation);
        }

        wrapper.orderByDesc(OperationLog::getCreateTime);
        return page(pageResult, wrapper);
    }
}
