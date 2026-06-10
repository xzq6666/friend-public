package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.JobKeyword;
import java.util.List;
import java.util.Map;

/**
 * 职位关键词服务
 */
public interface JobKeywordService extends IService<JobKeyword> {
    /**
     * 从文本中自动提取关键词
     * @param text 职位描述或任职要求文本
     * @param source 来源：SYSTEM-系统提取, MANUAL-手动添加, AI-AI分析
     * @return 提取到的关键词列表
     */
    List<JobKeyword> extractKeywordsFromText(String text, String source);

    /**
     * 为职位关联关键词
     * @param jobId 职位ID
     * @param text 职位描述或任职要求文本
     * @param source 来源：SYSTEM-系统提取, MANUAL-手动添加, AI-AI分析
     * @return 关联成功数量
     */
    int linkKeywordsToJob(Long jobId, String text, String source);

    /**
     * 获取所有关键词（按分类分组）
     */
    Map<String, List<JobKeyword>> getKeywordsByCategory();

    /**
     * 获取热门关键词（按使用次数排序）
     * @param limit 返回数量
     */
    List<JobKeyword> getTopKeywords(int limit);

    /**
     * 同步简历技能到关键词库
     * 已有的增加使用次数，没有的AI智能分类后自动新增
     * @param skills 技能列表
     */
    void syncResumeSkills(List<String> skills);
}
