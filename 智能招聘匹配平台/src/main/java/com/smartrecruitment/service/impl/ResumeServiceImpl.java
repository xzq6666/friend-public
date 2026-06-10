package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.mapper.ResumeMapper;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.AIService;
import com.smartrecruitment.service.DocumentParseService;
import com.smartrecruitment.service.FileUploadService;
import com.smartrecruitment.service.JobKeywordService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);
    private static final String AI_CACHE_PREFIX = "ai:analysis:";
    private static final int AI_CACHE_HOURS = 24; // AI分析结果缓存24小时
    private static final int MAX_AI_RETRY = 3;    // AI分析最大重试次数

    @Autowired
    private AIService aiService;

    @Autowired
    private DocumentParseService documentParseService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private JobKeywordService jobKeywordService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean updateById(Resume resume) {
        if (resume.getSkills() != null) {
            resume.setSkills(ensureValidJsonArray(resume.getSkills()));
        }
        if (resume.getWorkExperience() != null) {
            resume.setWorkExperience(ensureValidJsonArray(resume.getWorkExperience()));
        }
        boolean result = super.updateById(resume);
        syncSkillsToKeywords(resume.getSkills());
        // 清除AI分析缓存，下次重新分析
        if (resume.getId() != null) {
            evictAiCache(resume.getId());
            evictMatchRecommendCache(resume.getId());
        }
        return result;
    }

    @Override
    public Resume analyzeResumeWithAI(Long resumeId) {
        return analyzeResumeWithAI(resumeId, false);
    }

    @Override
    public Resume analyzeResumeWithAI(Long resumeId, boolean forceRefresh) {
        Resume resume = getById(resumeId);
        if (resume == null) return null;

        String cacheKey = AI_CACHE_PREFIX + resumeId;

        // 非强制刷新时，先查缓存
        if (!forceRefresh) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.info("命中AI分析缓存，简历ID: {}", resumeId);
                    resume.setAiAnalysis(cached.toString());
                    return resume;
                }
            } catch (Exception e) {
                log.warn("读取AI缓存失败: {}", e.getMessage());
            }
        }

        // 带重试的AI分析
        String analysisResult = analyzeWithRetry(resume, MAX_AI_RETRY);

        if (analysisResult != null) {
            analysisResult = cleanAnalysisJson(analysisResult);
            resume.setAiAnalysis(analysisResult);
            updateById(resume);

            // 写入缓存
            try {
                redisTemplate.opsForValue().set(cacheKey, analysisResult, AI_CACHE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("写入AI缓存失败: {}", e.getMessage());
            }
        }

        return resume;
    }

    /**
     * 带重试机制的AI分析
     */
    private String analyzeWithRetry(Resume resume, int maxRetry) {
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                log.info("AI分析第{}次尝试，简历ID: {}", attempt, resume.getId());
                String result = aiService.analyzeResume(resume);
                if (result != null && !result.trim().isEmpty() && !result.equals("{}")) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("AI分析第{}次尝试失败: {}", attempt, e.getMessage());
                if (attempt < maxRetry) {
                    try {
                        Thread.sleep(1000L * attempt); // 递增等待
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        log.error("AI分析全部重试失败，简历ID: {}", resume.getId());
        return null;
    }

    @Override
    public Resume createResume(Resume resume) {
        // 字段校验
        validateResume(resume);

        if (resume.getSkills() != null) {
            resume.setSkills(ensureValidJsonArray(resume.getSkills()));
        }
        if (resume.getWorkExperience() != null) {
            resume.setWorkExperience(ensureValidJsonArray(resume.getWorkExperience()));
        }

        save(resume);
        syncSkillsToKeywords(resume.getSkills());

        // 异步触发AI分析，不阻塞创建流程
        final Long resumeId = resume.getId();
        new Thread(() -> {
            try {
                analyzeResumeWithAI(resumeId);
            } catch (Exception e) {
                log.warn("创建后异步AI分析失败，简历ID: {}", resumeId);
            }
        }, "ai-analysis-" + resumeId).start();

        return resume;
    }

    @Override
    public void validateResume(Resume resume) {
        List<String> errors = new ArrayList<>();

        // 姓名校验
        if (resume.getName() != null && resume.getName().length() > 50) {
            errors.add("姓名长度不能超过50个字符");
        }

        // 年龄校验
        if (resume.getAge() != null && (resume.getAge() < 16 || resume.getAge() > 80)) {
            errors.add("年龄必须在16-80岁之间");
        }

        // 技能格式校验
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            try {
                String normalized = ensureValidJsonArray(resume.getSkills());
                List<String> skills = objectMapper.readValue(normalized, new TypeReference<>() {});
                if (skills.size() > 50) {
                    errors.add("技能标签不能超过50个");
                }
            } catch (Exception e) {
                errors.add("技能格式不正确");
            }
        }

        // 自我评价长度校验
        if (resume.getSelfIntroduction() != null && resume.getSelfIntroduction().length() > 2000) {
            errors.add("自我评价不能超过2000个字符");
        }

        // 工作经历长度校验
        if (resume.getWorkExperience() != null && resume.getWorkExperience().length() > 10000) {
            errors.add("工作经历内容过长");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private void syncSkillsToKeywords(String skillsJson) {
        if (skillsJson == null || skillsJson.isEmpty() || "[]".equals(skillsJson)) return;
        try {
            List<String> rawSkills = objectMapper.readValue(skillsJson, new TypeReference<>() {});
            if (rawSkills == null || rawSkills.isEmpty()) return;

            // 展开：有些元素可能是逗号拼接的整串，需要拆开
            List<String> skills = new ArrayList<>();
            for (String raw : rawSkills) {
                if (raw == null) continue;
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) continue;
                // 如果单个元素包含逗号、顿号、分号，说明是多个技能拼在一起，需要拆分
                if (trimmed.contains(",") || trimmed.contains("，") || trimmed.contains("、") || trimmed.contains(";") || trimmed.contains("；")) {
                    for (String part : trimmed.split("[,，、;；]+")) {
                        String p = part.trim();
                        if (!p.isEmpty()) skills.add(p);
                    }
                } else {
                    skills.add(trimmed);
                }
            }

            if (!skills.isEmpty()) {
                jobKeywordService.syncResumeSkills(skills);
            }
        } catch (Exception e) {
            log.error("syncSkillsToKeywords失败, skillsJson={}", skillsJson, e);
        }
    }

    private String ensureValidJsonArray(String str) {
        if (str == null || str.isEmpty()) return "[]";
        str = str.trim();
        if (str.startsWith("[") && str.endsWith("]")) {
            try {
                List<String> list = objectMapper.readValue(str, new TypeReference<>() {});
                List<String> expanded = new ArrayList<>();
                for (String item : list) {
                    if (item == null) continue;
                    String trimmed = item.trim();
                    if (trimmed.isEmpty()) continue;
                    if (trimmed.contains(",") || trimmed.contains("，") || trimmed.contains("、") || trimmed.contains(";") || trimmed.contains("；")) {
                        for (String part : trimmed.split("[,，、;；]+")) {
                            String p = part.trim();
                            if (!p.isEmpty()) expanded.add(p);
                        }
                    } else {
                        expanded.add(trimmed);
                    }
                }
                if (expanded.size() == list.size()) return str; // 没有变化，原样返回
                // 重新构建JSON数组
                StringBuilder rebuild = new StringBuilder("[");
                for (int i = 0; i < expanded.size(); i++) {
                    if (i > 0) rebuild.append(",");
                    rebuild.append("\"").append(expanded.get(i).replace("\"", "\\\"")).append("\"");
                }
                rebuild.append("]");
                return rebuild.toString();
            } catch (Exception e) {
                return str; // 解析失败原样返回
            }
        }
        String[] items = str.split("[,，、;；]+");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (!item.isEmpty()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("\"").append(item.replace("\"", "\\\"")).append("\"");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Resume getResumeByUserId(Long userId) {
        // 兼容旧方法，返回默认简历
        return getDefaultResumeByUserId(userId);
    }

    @Override
    public List<Resume> getResumesByUserId(Long userId) {
        return lambdaQuery()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getIsDefault)
                .orderByDesc(Resume::getCreateTime)
                .list();
    }

    @Override
    public Resume getDefaultResumeByUserId(Long userId) {
        // 先查找默认简历
        Resume defaultResume = lambdaQuery()
                .eq(Resume::getUserId, userId)
                .eq(Resume::getIsDefault, 1)
                .last("LIMIT 1")
                .one();
        
        // 如果没有默认简历，返回最新的简历
        if (defaultResume == null) {
            return lambdaQuery()
                    .eq(Resume::getUserId, userId)
                    .orderByDesc(Resume::getCreateTime)
                    .last("LIMIT 1")
                    .one();
        }
        
        return defaultResume;
    }

    @Override
    public Resume importResumeFromDocument(Long userId, MultipartFile file) {
        String fileUrl = fileUploadService.upload(file);
        Resume parsedResume = documentParseService.parseResumeToEntity(file);
        parsedResume.setUserId(userId);
        parsedResume.setFileUrl(fileUrl);
        save(parsedResume);
        syncSkillsToKeywords(parsedResume.getSkills());

        // 异步深度AI分析
        final Long resumeId = parsedResume.getId();
        new Thread(() -> {
            try {
                analyzeResumeWithAI(resumeId);
            } catch (Exception e) {
                log.warn("导入后异步AI分析失败，简历ID: {}", resumeId);
            }
        }, "ai-analysis-" + resumeId).start();

        return parsedResume;
    }

    @Override
    public long countByCreateTime(LocalDateTime createTime) {
        LocalDateTime startOfDay = createTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return lambdaQuery().ge(Resume::getCreateTime, startOfDay).lt(Resume::getCreateTime, endOfDay).count();
    }

    @Override
    public long countByDateRange(LocalDateTime start, LocalDateTime end) {
        return lambdaQuery().ge(Resume::getCreateTime, start).lt(Resume::getCreateTime, end).count();
    }

    @Override
    public void deepAnalyzeResumeAsync(Long resumeId, String rawText) {
        log.info("开始异步AI深度解析，简历ID: {}", resumeId);
        Resume resume = getById(resumeId);
        if (resume == null) {
            log.warn("简历不存在，ID: {}", resumeId);
            return;
        }

        String analysisResult = analyzeWithRetry(resume, MAX_AI_RETRY);
        if (analysisResult != null) {
            analysisResult = cleanAnalysisJson(analysisResult);
            resume.setAiAnalysis(analysisResult);
            updateById(resume);

            // 更新缓存
            try {
                redisTemplate.opsForValue().set(AI_CACHE_PREFIX + resumeId, analysisResult, AI_CACHE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("写入AI缓存失败: {}", e.getMessage());
            }
            log.info("异步AI解析完成，简历ID: {}", resumeId);
        }
    }

    @Override
    public List<Map<String, Object>> getTopSkills(int limit) {
        List<Resume> resumes = list();
        Map<String, Integer> skillCount = new HashMap<>();

        for (Resume resume : resumes) {
            if (resume.getSkills() != null) {
                try {
                    String skillsJson = resume.getSkills();
                    List<String> skills = objectMapper.readValue(skillsJson, new TypeReference<>() {});
                    for (String skill : skills) {
                        String trimmed = skill.trim();
                        if (!trimmed.isEmpty()) {
                            skillCount.merge(trimmed, 1, Integer::sum);
                        }
                    }
                } catch (Exception e) {
                    // 降级：简单分割
                    String[] skills = resume.getSkills().replaceAll("[\\[\\]{}\"]", "").split(",");
                    for (String skill : skills) {
                        String trimmed = skill.trim();
                        if (!trimmed.isEmpty()) {
                            skillCount.merge(trimmed, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        return skillCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> skillData = new HashMap<>();
                    skillData.put("skill", entry.getKey());
                    skillData.put("count", entry.getValue());
                    return skillData;
                })
                .collect(Collectors.toList());
    }

    /**
     * 清除AI分析缓存
     */
    private void evictAiCache(Long resumeId) {
        try {
            redisTemplate.delete(AI_CACHE_PREFIX + resumeId);
        } catch (Exception e) {
            log.warn("清除AI缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除匹配推荐缓存（简历变更时调用）
     */
    private void evictMatchRecommendCache(Long resumeId) {
        try {
            redisTemplate.delete("match:recommend:resume:" + resumeId);
            log.info("已清除匹配推荐缓存: resumeId={}", resumeId);
        } catch (Exception e) {
            log.warn("清除匹配推荐缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清理AI分析JSON
     */
    private String cleanAnalysisJson(String analysisResult) {
        if (analysisResult == null) return null;
        return analysisResult
                .replaceAll("```(?:json)?\\s*", "")
                .replaceAll("```\\s*$", "")
                .replaceAll(",\\s*([}\\]])", "$1")
                .trim();
    }
}
