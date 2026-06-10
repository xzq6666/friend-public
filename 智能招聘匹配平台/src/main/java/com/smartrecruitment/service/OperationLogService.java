package com.smartrecruitment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 记录操作日志
     */
    void log(Long userId, String username, String operation, String module, String description, String ipAddress, Integer status);

    /**
     * 分页查询操作日志
     */
    IPage<OperationLog> getLogPage(int page, int size, String username, String module, String operation);
}
