package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Resume;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ================================================
 * 简历服务接口
 * ================================================
 *
 * 提供简历相关的业务逻辑抽象。
 *
 * 如需扩展：
 *   - 添加 getResumesByUserId(Long userId) 获取用户的所有简历
 *   - 添加 batchAnalyze() 批量 AI 分析未处理的简历
 *   - 添加 searchResumes(keyword, filters, page) 高级搜索
 *   - 添加 exportResume(Long id, format) 导出简历（PDF/Word）
 *   - 添加 compareResumes(Long id1, Long id2) 简历对比
 *   - 添加 getResumeStatistics() 简历统计数据
 *   - 添加 importResume(file) 文件导入简历（解析 docx/pdf）
 */
public interface ResumeService extends IService<Resume> {
    Resume analyzeResumeWithAI(Long resumeId);
    Resume analyzeResumeWithAI(Long resumeId, boolean forceRefresh);
    Resume createResume(Resume resume);
    Resume getResumeByUserId(Long userId);
    
    /**
     * 获取用户的所有简历列表
     */
    List<Resume> getResumesByUserId(Long userId);
    
    /**
     * 获取用户的默认简历
     */
    Resume getDefaultResumeByUserId(Long userId);
    
    Resume importResumeFromDocument(Long userId, org.springframework.web.multipart.MultipartFile file);

    // 数据统计方法
    long countByCreateTime(LocalDateTime createTime);
    long countByDateRange(LocalDateTime start, LocalDateTime end);
    List<Map<String, Object>> getTopSkills(int limit);

    // 异步深度AI解析（上传后调用，不阻塞用户）
    void deepAnalyzeResumeAsync(Long resumeId, String rawText);

    // 简历字段校验
    void validateResume(Resume resume);
}
