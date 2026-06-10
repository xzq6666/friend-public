package com.smartrecruitment.service.impl;

import com.smartrecruitment.config.BailianConfig;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobSubscription;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.service.AIService;
import com.smartrecruitment.service.MatchRecordService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * AI 服务实现 - 增强版
 *
 * 架构：
 * - L1: 基础分析（简历分析、规则匹配、面试题、推荐）
 * - L2: 混合匹配引擎（AI语义匹配 + 规则引擎加权融合）
 * - L3: 高级功能（求职信、简历优化、薪资预测、职业路径、简历对比）
 */
@Service
public class BailianAIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(BailianAIServiceImpl.class);

    @Autowired
    private BailianConfig bailianConfig;

    @Lazy
    @Autowired
    private MatchRecordService matchRecordService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== L1 基础分析 ====================

    @Override
    public String analyzeResume(Resume resume) {
        String prompt = buildAnalyzePrompt(resume);
        return cleanJsonResponse(bailianConfig.call(prompt));
    }

    @Override
    public BigDecimal calculateMatchScore(Resume resume, Job job) {
        String prompt = buildMatchPrompt(resume, job);
        String result = bailianConfig.call(prompt, false);
        try {
            String number = result.replaceAll("[^0-9.]", "").trim();
            if (!number.isEmpty()) {
                BigDecimal score = new BigDecimal(number);
                if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
                if (score.compareTo(new BigDecimal(100)) > 0) return new BigDecimal(100);
                return score.setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("AI匹配评分解析失败: {}", result);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String generateInterviewQuestions(Job job, Resume resume) {
        String prompt = buildInterviewPrompt(job, resume);
        return cleanJsonResponse(bailianConfig.call(prompt));
    }

    @Override
    public String generateJobRecommendations(List<Job> jobs, Resume resume) {
        String prompt = buildRecommendPrompt(jobs, resume);
        return cleanJsonResponse(bailianConfig.call(prompt));
    }

    // ==================== L2 混合匹配引擎 ====================

    @Override
    public BigDecimal calculateSemanticMatchScore(Resume resume, Job job) {
        String prompt = buildSemanticMatchPrompt(resume, job);
        String result = bailianConfig.call(prompt);
        try {
            JsonNode node = objectMapper.readTree(result);
            JsonNode scoreNode = node.get("semantic_score");
            if (scoreNode != null && scoreNode.isNumber()) {
                BigDecimal score = scoreNode.decimalValue();
                return score.max(BigDecimal.ZERO).min(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
            }
            // 尝试从文本中提取数字
            String number = result.replaceAll("[^0-9.]", "").trim();
            if (!number.isEmpty()) {
                return new BigDecimal(number).max(BigDecimal.ZERO).min(new BigDecimal(100));
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("AI语义匹配评分解析失败: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Map<String, Object> hybridMatch(Resume resume, Job job) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 规则引擎分数（同步计算）
        Map<String, Object> ruleDetail = calculateRuleBasedDetail(resume, job);
        double ruleScore = Double.parseDouble(ruleDetail.get("total").toString());

        // 2. AI语义分数（调用大模型）
        BigDecimal aiScore = calculateSemanticMatchScore(resume, job);

        // 3. v10优化：动态权重融合 - 根据分数区间调整AI权重
        double aiWeight = calculateDynamicAiWeight(ruleScore);
        double ruleWeight = 1.0 - aiWeight;
        
        double hybridScore = ruleScore * ruleWeight + aiScore.doubleValue() * aiWeight;
        hybridScore = Math.min(100, Math.max(0, hybridScore));

        // 4. 置信度评估（增强版）
        double confidence = evaluateConfidence(ruleScore, aiScore.doubleValue(), resume, job);
        
        // 5. 生成推荐建议
        String recommendation = generateHybridRecommendation(hybridScore, confidence, ruleScore, aiScore.doubleValue());

        result.put("hybridScore", Double.parseDouble(String.format("%.1f", hybridScore)));
        result.put("ruleScore", Double.parseDouble(String.format("%.1f", ruleScore)));
        result.put("aiScore", aiScore);
        result.put("confidence", Double.parseDouble(String.format("%.2f", confidence)));
        result.put("recommendation", recommendation);
        result.put("ruleDetail", ruleDetail);
        result.put("weights", Map.of("rule", round(ruleWeight), "ai", round(aiWeight)));

        return result;
    }
    
    /**
     * v10新增：动态AI权重计算
     * 根据规则引擎分数区间，动态调整AI权重
     */
    private double calculateDynamicAiWeight(double ruleScore) {
        if (ruleScore < 30) {
            // 极低分：快速拒绝，不调用AI节省成本（但已调用，降低权重）
            return 0.2;
        } else if (ruleScore < 50) {
            // 中低分：AI重点审查，提高权重以发现潜在人才
            return 0.6;
        } else if (ruleScore > 85) {
            // 高分段：AI验证是否存在隐藏问题
            return 0.5;
        } else {
            // 常规区间：标准混合
            return 0.4;
        }
    }
    
    /**
     * v10新增：基于混合结果生成推荐建议
     */
    private String generateHybridRecommendation(double hybridScore, double confidence, double ruleScore, double aiScore) {
        StringBuilder sb = new StringBuilder();
        
        // 基于总分
        if (hybridScore >= 80) {
            sb.append("强烈推荐");
        } else if (hybridScore >= 65) {
            sb.append("推荐");
        } else if (hybridScore >= 50) {
            sb.append("可考虑");
        } else if (hybridScore >= 35) {
            sb.append("谨慎考虑");
        } else {
            sb.append("不推荐");
        }
        
        // 基于置信度
        if (confidence < 0.5) {
            sb.append("（需人工复核");
            double divergence = Math.abs(ruleScore - aiScore);
            if (divergence > 30) {
                sb.append("，规则与AI评分分歧较大");
            }
            sb.append("）");
        }
        
        // 特殊情况提示
        if (ruleScore >= 70 && aiScore < 50) {
            sb.append(" [注意：规则评分高但AI认为匹配度一般]");
        } else if (ruleScore < 50 && aiScore >= 70) {
            sb.append(" [潜力候选人：AI发现隐藏优势]");
        }
        
        return sb.toString();
    }

    @Override
    public String generateMatchExplanation(Resume resume, Job job, Map<String, Object> matchDetail) {
        String prompt = buildMatchExplanationPrompt(resume, job, matchDetail);
        return bailianConfig.call(prompt);
    }

    // ==================== L3 高级功能 ====================

    @Override
    public String generateCoverLetter(Resume resume, Job job) {
        String prompt = buildCoverLetterPrompt(resume, job);
        return bailianConfig.call(prompt, false); // 求职信不需要JSON格式
    }

    @Override
    public String optimizeResume(Resume resume, Job job) {
        String prompt = buildResumeOptimizePrompt(resume, job);
        return cleanJsonResponse(bailianConfig.call(prompt));
    }

    @Override
    public Map<String, Object> predictSalary(Resume resume) {
        String prompt = buildSalaryPredictPrompt(resume);
        String result = bailianConfig.call(prompt);
        try {
            return objectMapper.readValue(result, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("薪资预测解析失败: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "薪资预测解析失败");
            fallback.put("raw", result);
            return fallback;
        }
    }

    @Override
    public String generateCareerPath(Resume resume) {
        String prompt = buildCareerPathPrompt(resume);
        return cleanJsonResponse(bailianConfig.call(prompt));
    }

    @Override
    public String compareResumes(Resume resume1, Resume resume2) {
        String prompt = buildResumeComparePrompt(resume1, resume2);
        return cleanJsonResponse(bailianConfig.call(prompt));
    }

    // ==================== Prompt 构建方法 ====================

    /**
     * 构建简历分析Prompt - 精简优化版
     */
    private String buildAnalyzePrompt(Resume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是资深HR专家。分析以下简历，严格按JSON格式输出：\n\n");
        sb.append("{\n");
        sb.append("  \"total_score\": 总分0-100,\n");
        sb.append("  \"score_level\": \"优秀/良好/一般/较差\",\n");
        sb.append("  \"radar_data\": {\n");
        sb.append("    \"labels\": [\"基本信息\",\"技能水平\",\"工作经历\",\"教育背景\",\"职业潜力\"],\n");
        sb.append("    \"values\": [各维度实际得分],\n");
        sb.append("    \"max_values\": [15,35,30,10,10]\n");
        sb.append("  },\n");
        sb.append("  \"dimensions\": {\n");
        sb.append("    \"basic_info\": {\"score\": 得分, \"max\": 15, \"comment\": \"评价\"},\n");
        sb.append("    \"skill\": {\"score\": 得分, \"max\": 35, \"comment\": \"评价\"},\n");
        sb.append("    \"experience\": {\"score\": 得分, \"max\": 30, \"comment\": \"评价\"},\n");
        sb.append("    \"education\": {\"score\": 得分, \"max\": 10, \"comment\": \"评价\"},\n");
        sb.append("    \"potential\": {\"score\": 得分, \"max\": 10, \"comment\": \"评价\"}\n");
        sb.append("  },\n");
        sb.append("  \"strengths\": [\"优势1\",\"优势2\",\"优势3\"],\n");
        sb.append("  \"weaknesses\": [\"不足1\",\"不足2\"],\n");
        sb.append("  \"career_suggestions\": {\n");
        sb.append("    \"recommended_positions\": [\"职位1\",\"职位2\",\"职位3\"],\n");
        sb.append("    \"salary_range\": \"15K-25K\",\n");
        sb.append("    \"development_path\": \"发展路径\"\n");
        sb.append("  },\n");
        sb.append("  \"overall_comment\": \"综合评价(50-100字)\"\n");
        sb.append("}\n\n");
        sb.append("\n\n【重要】必须输出合法的JSON格式，不要包含任何Markdown标记（如```json），直接输出JSON对象。");

        sb.append("评分标准：\n");
        sb.append("- 基本信息(15): 姓名+联系方式5, 年龄2, 学历4, 期望薪资2, 经验年限2\n");
        sb.append("- 技能(35): 数量多样性10, 专业深度12, 行业匹配8, 证书5\n");
        sb.append("- 经历(30): 数量连续性6, 稳定性6, 公司背景8, 描述质量10\n");
        sb.append("- 教育(10): 学历层次6, 专业相关4\n");
        sb.append("- 潜力(10): 自我评价6, 职业规划4\n");
        sb.append("扣分：技能空->该维度0分上限50; 经历空->该维度0分上限40; 总字数<100->上限30\n\n");

        sb.append("简历数据：\n");
        sb.append("姓名：").append(ns(resume.getName())).append("\n");
        sb.append("年龄：").append(resume.getAge() != null ? resume.getAge() + "岁" : "空").append("\n");
        sb.append("学历：").append(ns(resume.getEducation())).append("\n");
        sb.append("期望薪资：").append(resume.getExpectedSalary() != null ? resume.getExpectedSalary() + "元" : "空").append("\n");
        sb.append("经验年限：").append(ns(resume.getExperience())).append("\n");
        sb.append("技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("工作经历：").append(ns(resume.getWorkExperience())).append("\n");
        sb.append("自我评价：").append(ns(resume.getSelfIntroduction())).append("\n");

        return sb.toString();
    }

    /**
     * 构建匹配评分Prompt - 精简优化版
     */
    private String buildMatchPrompt(Resume resume, Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("计算简历与职位的匹配度，只返回0-100的数字。\n\n");
        sb.append("评分维度：\n");
        sb.append("1. 行业匹配(25分): 同行业25, 相关行业12.5, 不同3\n");
        sb.append("2. 技能匹配(30分): 精确匹配权重80%, 模糊匹配20%\n");
        sb.append("3. 经验匹配(20分): 年限符合度, 经验内容相关性\n");
        sb.append("4. 学历匹配(10分): 达标10, 低一级5, 低两级2, 更低0\n");
        sb.append("5. 薪资匹配(8分): 期望在范围内8, 略高/略低适当降分\n");
        sb.append("6. 地点匹配(7分): 同城7, 一线城市互通5, 不同1\n\n");
        sb.append("降权规则：行业不匹配+技能<15 -> 总分上限35\n\n");

        sb.append("简历：分类ID=").append(resume.getCategoryId() != null ? resume.getCategoryId() : "无")
          .append(" 技能=").append(ns(resume.getSkills()))
          .append(" 经验=").append(ns(resume.getExperience()))
          .append(" 学历=").append(ns(resume.getEducation())).append("\n");
        sb.append("职位：分类ID=").append(job.getCategoryId() != null ? job.getCategoryId() : "无")
          .append(" 名称=").append(ns(job.getTitle()))
          .append(" 要求=").append(ns(job.getRequirements()))
          .append(" 经验要求=").append(ns(job.getExperienceRequired()))
          .append(" 学历要求=").append(ns(job.getEducationRequired())).append("\n");

        return sb.toString();
    }

    /**
     * 构建AI语义匹配Prompt - 评估深层语义相关性
     */
    private String buildSemanticMatchPrompt(Resume resume, Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是招聘专家，评估候选人与职位的深层语义匹配度。\n\n");
        sb.append("请从以下维度深度分析（不只看关键词，要理解语义）：\n");
        sb.append("1. 技能深度匹配：候选人的技能深度是否满足职位的技术要求\n");
        sb.append("2. 经验质量匹配：工作经历的质量、项目复杂度是否匹配职位层级\n");
        sb.append("3. 职业方向一致性：候选人的职业轨迹是否朝向该职位方向\n");
        sb.append("4. 发展潜力匹配：候选人的学习能力和成长空间是否适合该职位\n");
        sb.append("5. 文化契合度：从简历风格和内容判断是否适合该类型公司\n\n");
        sb.append("严格按JSON输出：\n");
        sb.append("{\n");
        sb.append("  \"semantic_score\": 0-100,\n");
        sb.append("  \"skill_depth_match\": 0-100,\n");
        sb.append("  \"experience_quality_match\": 0-100,\n");
        sb.append("  \"career_direction_match\": 0-100,\n");
        sb.append("  \"potential_match\": 0-100,\n");
        sb.append("  \"key_insights\": [\"关键发现1\", \"关键发现2\"],\n");
        sb.append("  \"risk_factors\": [\"风险点1\"]\n");
        sb.append("}\n\n");
        sb.append("\n\n【重要】必须输出合法的JSON格式，不要包含任何Markdown标记，直接输出JSON对象。");

        sb.append("候选人简历：\n");
        sb.append("姓名：").append(ns(resume.getName())).append("\n");
        sb.append("技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("学历：").append(ns(resume.getEducation())).append("\n");
        sb.append("工作经历：").append(ns(resume.getWorkExperience())).append("\n");
        sb.append("自我评价：").append(ns(resume.getSelfIntroduction())).append("\n\n");

        sb.append("目标职位：\n");
        sb.append("名称：").append(ns(job.getTitle())).append("\n");
        sb.append("描述：").append(ns(job.getDescription())).append("\n");
        sb.append("要求：").append(ns(job.getRequirements())).append("\n");
        sb.append("经验要求：").append(ns(job.getExperienceRequired())).append("\n");
        sb.append("学历要求：").append(ns(job.getEducationRequired())).append("\n");

        return sb.toString();
    }

    /**
     * 构建匹配解释Prompt
     */
    private String buildMatchExplanationPrompt(Resume resume, Job job, Map<String, Object> matchDetail) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是招聘专家，用简洁的中文解释以下匹配结果（100-150字）：\n\n");
        sb.append("候选人：").append(ns(resume.getName()))
          .append(" 技能：").append(ns(resume.getSkills()))
          .append(" 经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("目标职位：").append(ns(job.getTitle()))
          .append(" 要求：").append(ns(job.getRequirements())).append("\n");
        sb.append("匹配分数：").append(matchDetail.get("total")).append("/100\n");
        sb.append("各维度：行业=").append(matchDetail.get("industry"))
          .append(" 技能=").append(matchDetail.get("skill"))
          .append(" 经验=").append(matchDetail.get("experience"))
          .append(" 学历=").append(matchDetail.get("education"))
          .append(" 薪资=").append(matchDetail.get("salary"))
          .append(" 地点=").append(matchDetail.get("location")).append("\n\n");
        sb.append("请直接输出解释文字，不要JSON格式。");

        return sb.toString();
    }

    /**
     * 构建面试题生成Prompt
     */
    private String buildInterviewPrompt(Job job, Resume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是专业面试官。为以下候选人生成5个面试问题，按JSON输出：\n");
        sb.append("{\"questions\": [\"问题1\",\"问题2\",\"问题3\",\"问题4\",\"问题5\"]}\n\n");
        sb.append("职位：").append(ns(job.getTitle())).append("\n");
        sb.append("要求：").append(ns(job.getRequirements())).append("\n");
        sb.append("候选人技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("候选人经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("问题应包含：2个技术深度问题、2个经验验证问题、1个情景模拟问题。");

        return sb.toString();
    }

    /**
     * 构建职位推荐Prompt
     */
    private String buildRecommendPrompt(List<Job> jobs, Resume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是职业规划师。从以下职位中推荐最合适的3个，按JSON输出：\n");
        sb.append("{\"recommendations\": [{\"job_id\": ID, \"job_title\": \"职位名\", \"reason\": \"推荐理由(30字内)\"}]}\n\n");
        sb.append("候选人：技能=").append(ns(resume.getSkills()))
          .append(" 经验=").append(ns(resume.getExperience()))
          .append(" 期望薪资=").append(resume.getExpectedSalary() != null ? resume.getExpectedSalary() + "元" : "未填").append("\n\n");
        sb.append("职位列表：\n");
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            sb.append(i + 1).append(". ID=").append(j.getId())
              .append(" ").append(ns(j.getTitle()))
              .append(" 薪资:").append(j.getSalaryMin()).append("-").append(j.getSalaryMax())
              .append(" 地点:").append(ns(j.getLocation())).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建求职信生成Prompt
     */
    private String buildCoverLetterPrompt(Resume resume, Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名专业的求职顾问。为以下候选人撰写一封求职信（300-400字）：\n\n");
        sb.append("候选人信息：\n");
        sb.append("姓名：").append(ns(resume.getName())).append("\n");
        sb.append("技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("学历：").append(ns(resume.getEducation())).append("\n");
        sb.append("自我评价：").append(ns(resume.getSelfIntroduction())).append("\n\n");
        sb.append("目标职位：").append(ns(job.getTitle())).append("\n");
        sb.append("职位描述：").append(ns(job.getDescription())).append("\n");
        sb.append("职位要求：").append(ns(job.getRequirements())).append("\n\n");
        sb.append("要求：\n");
        sb.append("1. 开头表明求职意向和来源\n");
        sb.append("2. 中间段落突出与职位最匹配的2-3个核心优势\n");
        sb.append("3. 结尾表达期待和感谢\n");
        sb.append("4. 语气专业但不生硬\n");
        sb.append("5. 不要使用夸张的形容词\n");
        sb.append("直接输出求职信正文。");

        return sb.toString();
    }

    /**
     * 构建简历优化Prompt
     */
    private String buildResumeOptimizePrompt(Resume resume, Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是简历优化专家。针对以下职位要求，分析候选人简历的不足并给出优化建议。\n");
        sb.append("严格按JSON输出：\n");
        sb.append("{\n");
        sb.append("  \"overall_fit\": \"整体匹配度评价(20字)\",\n");
        sb.append("  \"missing_skills\": [\"缺失技能1\", \"缺失技能2\"],\n");
        sb.append("  \"optimizations\": [\n");
        sb.append("    {\"section\": \"技能/经历/评价\", \"current\": \"当前内容\", \"suggested\": \"优化建议\", \"priority\": \"高/中/低\"}\n");
        sb.append("  ],\n");
        sb.append("  \"keywords_to_add\": [\"建议添加的关键词\"],\n");
        sb.append("  \"overall_suggestion\": \"综合优化建议(50字)\"\n");
        sb.append("}\n\n");
        sb.append("候选人简历：\n");
        sb.append("技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("学历：").append(ns(resume.getEducation())).append("\n");
        sb.append("自我评价：").append(ns(resume.getSelfIntroduction())).append("\n");
        sb.append("工作经历：").append(ns(resume.getWorkExperience())).append("\n\n");
        sb.append("目标职位：").append(ns(job.getTitle())).append("\n");
        sb.append("要求：").append(ns(job.getRequirements())).append("\n");

        return sb.toString();
    }

    /**
     * 构建薪资预测Prompt
     */
    private String buildSalaryPredictPrompt(Resume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是薪资顾问。基于候选人简历预测合理薪资范围。\n");
        sb.append("严格按JSON输出：\n");
        sb.append("{\n");
        sb.append("  \"predicted_min\": 最低薪资数字,\n");
        sb.append("  \"predicted_max\": 最高薪资数字,\n");
        sb.append("  \"predicted_mid\": 中位数薪资数字,\n");
        sb.append("  \"currency\": \"CNY\",\n");
        sb.append("  \"unit\": \"月\",\n");
        sb.append("  \"factors\": {\"skill_value\": \"技能价值评估\", \"experience_premium\": \"经验溢价\", \"education_premium\": \"学历溢价\"},\n");
        sb.append("  \"market_comparison\": \"与市场对比(20字)\",\n");
        sb.append("  \"negotiation_tip\": \"薪资谈判建议(30字)\"\n");
        sb.append("}\n\n");
        sb.append("候选人：\n");
        sb.append("技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("学历：").append(ns(resume.getEducation())).append("\n");
        sb.append("当前期望：").append(resume.getExpectedSalary() != null ? resume.getExpectedSalary() + "元" : "未填").append("\n");

        return sb.toString();
    }

    /**
     * 构建职业路径Prompt
     */
    private String buildCareerPathPrompt(Resume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是职业规划师。基于候选人当前背景，规划职业发展路径。\n");
        sb.append("严格按JSON输出：\n");
        sb.append("{\n");
        sb.append("  \"current_level\": \"当前职级评估\",\n");
        sb.append("  \"short_term_1_2_years\": {\"target\": \"短期目标\", \"actions\": [\"行动1\",\"行动2\"]},\n");
        sb.append("  \"mid_term_3_5_years\": {\"target\": \"中期目标\", \"actions\": [\"行动1\",\"行动2\"]},\n");
        sb.append("  \"long_term_5_plus_years\": {\"target\": \"长期愿景\", \"actions\": [\"行动1\"]},\n");
        sb.append("  \"skill_gaps_to_address\": [\"需要补充的技能\"],\n");
        sb.append("  \"recommended_certifications\": [\"推荐证书\"]\n");
        sb.append("}\n\n");
        sb.append("候选人：\n");
        sb.append("技能：").append(ns(resume.getSkills())).append("\n");
        sb.append("经验：").append(ns(resume.getExperience())).append("\n");
        sb.append("学历：").append(ns(resume.getEducation())).append("\n");
        sb.append("自我评价：").append(ns(resume.getSelfIntroduction())).append("\n");

        return sb.toString();
    }

    /**
     * 构建简历对比Prompt
     */
    private String buildResumeComparePrompt(Resume r1, Resume r2) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是招聘专家。对比两份简历，评估各自优势和适合的方向。\n");
        sb.append("严格按JSON输出：\n");
        sb.append("{\n");
        sb.append("  \"candidate_a_strengths\": [\"优势1\",\"优势2\"],\n");
        sb.append("  \"candidate_b_strengths\": [\"优势1\",\"优势2\"],\n");
        sb.append("  \"skill_comparison\": {\"a_unique\": [\"A独有技能\"], \"b_unique\": [\"B独有技能\"], \"common\": [\"共同技能\"]},\n");
        sb.append("  \"experience_comparison\": \"经验对比(50字)\",\n");
        sb.append("  \"overall_assessment\": \"综合评估(50字)\",\n");
        sb.append("  \"recommendation\": \"如果要选一个，推荐谁？为什么？(30字)\"\n");
        sb.append("}\n\n");
        sb.append("简历A：\n");
        sb.append("姓名：").append(ns(r1.getName())).append(" 技能：").append(ns(r1.getSkills()))
          .append(" 经验：").append(ns(r1.getExperience())).append(" 学历：").append(ns(r1.getEducation())).append("\n\n");
        sb.append("简历B：\n");
        sb.append("姓名：").append(ns(r2.getName())).append(" 技能：").append(ns(r2.getSkills()))
          .append(" 经验：").append(ns(r2.getExperience())).append(" 学历：").append(ns(r2.getEducation())).append("\n");

        return sb.toString();
    }

    // ==================== 规则引擎（供混合匹配调用）====================

    /**
     * 计算规则引擎匹配详情（委托给 MatchRecordServiceImpl，统一权重）
     */
    private Map<String, Object> calculateRuleBasedDetail(Resume resume, Job job) {
        return matchRecordService.calculateMatchDetail(resume, job);
    }


    /**
     * v10增强：置信度评估 - 综合考虑多个因素
     */
    private double evaluateConfidence(double ruleScore, double aiScore, Resume resume, Job job) {
        // 1. 基础置信度：规则与AI的一致性
        double divergence = Math.abs(ruleScore - aiScore);
        double baseConfidence;
        
        if (divergence < 5) {
            baseConfidence = 0.95;  // 高度一致
        } else if (divergence < 10) {
            baseConfidence = 0.85;  // 基本一致
        } else if (divergence < 20) {
            baseConfidence = 0.70;  // 存在分歧
        } else if (divergence < 30) {
            baseConfidence = 0.50;  // 较大分歧
        } else {
            baseConfidence = 0.30;  // 严重分歧
        }
        
        // 2. 数据完整性因子
        double dataQualityFactor = calculateDataQualityFactor(resume, job);
        
        // 3. 极端分数惩罚（过高或过低都可能不准确）
        double extremeScoreFactor = 1.0;
        if (ruleScore > 90 || ruleScore < 20) {
            extremeScoreFactor = 0.9;  // 极端分数略微降低置信度
        }
        
        // 综合置信度
        double finalConfidence = baseConfidence * dataQualityFactor * extremeScoreFactor;
        return Math.max(0.1, Math.min(1.0, finalConfidence));
    }
    
    /**
     * v10新增：计算数据质量因子
     */
    private double calculateDataQualityFactor(Resume resume, Job job) {
        double factor = 1.0;
        
        // 简历完整度检查
        int missingFields = 0;
        if (resume.getSkills() == null || resume.getSkills().isEmpty()) missingFields++;
        if (resume.getExperience() == null || resume.getExperience().isEmpty()) missingFields++;
        if (resume.getEducation() == null || resume.getEducation().isEmpty()) missingFields++;
        if (resume.getWorkExperience() == null || resume.getWorkExperience().length() < 20) missingFields++;
        
        // 每缺少一个关键字段，降低10%置信度
        factor -= missingFields * 0.1;
        
        // 职位描述详细度
        String jobDesc = safe(job.getDescription()) + " " + safe(job.getRequirements());
        if (jobDesc.length() < 50) {
            factor -= 0.15;  // 职位描述过于简单
        } else if (jobDesc.length() < 150) {
            factor -= 0.05;  // 职位描述较简单
        }
        
        return Math.max(0.5, factor);  // 最低不低于0.5
    }
    
    private String safe(String value) {
        return value != null ? value : "";
    }
    
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ==================== 工具方法 ====================

    private String ns(String value) {
        return value != null && !value.isEmpty() ? value : "空";
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        if (!response.startsWith("{")) {
            int start = response.indexOf("{");
            if (start >= 0) response = response.substring(start);
        }
        if (!response.endsWith("}")) {
            int end = response.lastIndexOf("}");
            if (end >= 0) response = response.substring(0, end + 1);
        }
        // 移除JSON中常见的尾随逗号
        response = response.replaceAll(",\\s*([}\\]])", "$1");
        return response.isEmpty() ? "{}" : response;
    }

    // ==================== 订阅匹配 ====================

    @Override
    public List<Map<String, Object>> matchJobsForSubscription(JobSubscription subscription, List<Job> candidateJobs) {
        if (candidateJobs == null || candidateJobs.isEmpty()) {
            return Collections.emptyList();
        }

        String prompt = buildSubscriptionMatchPrompt(subscription, candidateJobs);
        String response = cleanJsonResponse(bailianConfig.call(prompt));

        List<Map<String, Object>> matches = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode matchesNode = root.get("matches");
            if (matchesNode != null && matchesNode.isArray()) {
                for (JsonNode match : matchesNode) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("job_id", match.has("job_id") ? match.get("job_id").asLong() : null);
                    item.put("reason", match.has("reason") ? match.get("reason").asText() : "");
                    if (item.get("job_id") != null) {
                        matches.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI订阅匹配结果解析失败: {}", response, e);
        }

        return matches;
    }

    private String buildSubscriptionMatchPrompt(JobSubscription subscription, List<Job> jobs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是招聘匹配专家。根据用户的职位订阅条件，从候选职位中找出所有匹配的职位。\n\n");

        sb.append("【订阅条件】\n");
        sb.append("订阅名称：").append(ns(subscription.getName())).append("\n");

        // 解析关键词
        if (subscription.getKeywords() != null && !subscription.getKeywords().isEmpty()) {
            try {
                List<String> keywords = objectMapper.readValue(subscription.getKeywords(), List.class);
                sb.append("关注关键词：").append(String.join("、", keywords)).append("\n");
            } catch (Exception e) {
                sb.append("关注关键词：").append(ns(subscription.getKeywords())).append("\n");
            }
        }

        if (subscription.getCategoryId() != null) {
            sb.append("行业分类ID：").append(subscription.getCategoryId()).append("\n");
        }
        if (subscription.getSalaryMin() != null || subscription.getSalaryMax() != null) {
            sb.append("期望薪资：")
              .append(subscription.getSalaryMin() != null ? subscription.getSalaryMin() : "不限")
              .append("-")
              .append(subscription.getSalaryMax() != null ? subscription.getSalaryMax() : "不限")
              .append("K\n");
        }
        if (subscription.getLocation() != null && !subscription.getLocation().isEmpty()) {
            sb.append("期望地点：").append(subscription.getLocation()).append("\n");
        }
        if (subscription.getExperienceRequired() != null && !subscription.getExperienceRequired().isEmpty()) {
            sb.append("经验要求：").append(subscription.getExperienceRequired()).append("\n");
        }
        if (subscription.getEducationRequired() != null && !subscription.getEducationRequired().isEmpty()) {
            sb.append("学历要求：").append(subscription.getEducationRequired()).append("\n");
        }

        sb.append("\n【匹配规则】\n");
        sb.append("- 职位标题语义相似也算匹配（如\"Java高级开发\"匹配\"Java资深工程师\"）\n");
        sb.append("- 技能要求有重叠即算匹配\n");
        sb.append("- 薪资范围有交集即算匹配\n");
        sb.append("- 宽松匹配：不要求完全满足所有条件，重点关注关键词和行业相关性\n\n");

        sb.append("【候选职位列表】\n");
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            sb.append(i + 1).append(". ID=").append(j.getId())
              .append(" | 标题：").append(ns(j.getTitle()))
              .append(" | 薪资：").append(j.getSalaryMin()).append("-").append(j.getSalaryMax()).append("K")
              .append(" | 地点：").append(ns(j.getLocation()))
              .append(" | 经验：").append(ns(j.getExperienceRequired()))
              .append(" | 学历：").append(ns(j.getEducationRequired()));
            if (j.getDescription() != null && !j.getDescription().isEmpty()) {
                String desc = j.getDescription().length() > 100 ? j.getDescription().substring(0, 100) + "..." : j.getDescription();
                sb.append(" | 描述：").append(desc);
            }
            sb.append("\n");
        }

        sb.append("\n请输出JSON格式结果（只输出JSON，不要其他内容）：\n");
        sb.append("{\"matches\": [{\"job_id\": 职位ID, \"reason\": \"匹配理由(20字内)\"}]}\n");
        sb.append("如果没有匹配的职位，返回 {\"matches\": []}");

        return sb.toString();
    }
}
