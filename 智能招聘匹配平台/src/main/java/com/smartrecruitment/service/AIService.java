package com.smartrecruitment.service;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobSubscription;
import com.smartrecruitment.entity.Resume;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 服务接口 - 智能招聘核心能力
 *
 * 能力分层：
 * L1 - 基础分析：简历分析、匹配评分、面试题生成、职位推荐
 * L2 - 混合匹配：AI语义匹配、混合匹配引擎、匹配解释生成
 * L3 - 高级功能：求职信生成、简历优化、薪资预测、职业路径、简历对比
 */
public interface AIService {

    // ==================== L1 基础分析 ====================

    /** 深度简历分析 - 多维度评分+优势不足+职业建议 */
    String analyzeResume(Resume resume);

    /** 规则引擎匹配评分 - 6维度算法（行业/技能/经验/学历/薪资/地点） */
    BigDecimal calculateMatchScore(Resume resume, Job job);

    /** 生成面试问题 - 基于职位要求和候选人背景 */
    String generateInterviewQuestions(Job job, Resume resume);

    /** 职位推荐 - 从候选职位中推荐最匹配的Top3 */
    String generateJobRecommendations(List<Job> jobs, Resume resume);

    // ==================== L2 混合匹配引擎 ====================

    /**
     * AI语义匹配评分 - 使用大模型评估简历与职位的深层语义相关性
     * 返回0-100分，考虑技能深度、经验质量、职业方向、发展潜力等难以规则化的维度
     */
    BigDecimal calculateSemanticMatchScore(Resume resume, Job job);

    /**
     * 混合匹配 - 融合规则引擎和AI语义匹配
     * @return 包含各维度分数和AI分数的详细结果
     */
    Map<String, Object> hybridMatch(Resume resume, Job job);

    /**
     * 生成匹配解释 - 用自然语言解释为什么匹配/不匹配
     */
    String generateMatchExplanation(Resume resume, Job job, Map<String, Object> matchDetail);

    // ==================== L3 高级功能 ====================

    /** 自动生成求职信 */
    String generateCoverLetter(Resume resume, Job job);

    /** 针对特定职位优化简历 - 返回优化建议 */
    String optimizeResume(Resume resume, Job job);

    /** 基于简历预测合理薪资范围 */
    Map<String, Object> predictSalary(Resume resume);

    /** 职业发展路径建议 */
    String generateCareerPath(Resume resume);

    /** AI对比两份简历 */
    String compareResumes(Resume resume1, Resume resume2);

    // ==================== 订阅匹配 ====================

    /** AI-powered job matching for subscription criteria */
    List<Map<String, Object>> matchJobsForSubscription(JobSubscription subscription, List<Job> candidateJobs);
}
