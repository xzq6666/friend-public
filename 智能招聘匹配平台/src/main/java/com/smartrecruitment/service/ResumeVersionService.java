package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.ResumeVersion;

import java.util.List;
import java.util.Map;

/**
 * 简历版本服务接口
 */
public interface ResumeVersionService extends IService<ResumeVersion> {
    
    /**
     * 创建新版本快照
     */
    ResumeVersion createVersion(Long resumeId, String tag, String note, Long userId);
    
    /**
     * 查询简历的所有版本
     */
    List<ResumeVersion> getVersions(Long resumeId);
    
    /**
     * 获取版本详情
     */
    ResumeVersion getVersionDetail(Long versionId);
    
    /**
     * 恢复到指定版本
     */
    boolean restoreToVersion(Long versionId, Long userId);
    
    /**
     * 对比两个版本
     */
    Map<String, Object> compareVersions(Long version1Id, Long version2Id);
    
    /**
     * 删除版本
     */
    boolean deleteVersion(Long versionId, Long userId);
    
    /**
     * 获取最新版本号
     */
    String getLatestVersionNumber(Long resumeId);
}
