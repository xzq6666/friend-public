package com.smartrecruitment.service;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.service.impl.MatchRecordServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步匹配服务 - 处理 AI 增强匹配等耗时操作
 */
@Service
public class AsyncMatchService {

    private static final Logger log = LoggerFactory.getLogger(AsyncMatchService.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 异步 AI 增强匹配分数（并行执行，完成后直接更新缓存中对应条目的分数）
     */
    @Async("aiTaskExecutor")
    public void asyncAiEnhance(Resume resume, List<Map<String, Object>> needsAiReview) {
        int aiLimit = Math.min(20, needsAiReview.size());
        String cacheKey = "match:recommend:resume:" + resume.getId() + ":limit" + 10;

        log.info("开始异步 AI 增强: resumeId={}, 待处理数量={}", resume.getId(), aiLimit);

        // 并行执行 AI 调用
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < aiLimit; i++) {
            final Map<String, Object> rec = needsAiReview.get(i);
            final Job job = (Job) rec.get("job");
            if (job == null) continue;

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> hybridResult = aiService.hybridMatch(resume, job);
                    double hybridScore = (double) hybridResult.getOrDefault("hybridScore", 0.0);
                    synchronized (rec) {
                        rec.put("matchScore", new BigDecimal(hybridScore).setScale(1, RoundingMode.HALF_UP));
                        rec.put("hybridDetail", hybridResult);
                        rec.put("aiReviewed", true);
                    }
                    log.debug("AI 增强完成: resumeId={}, jobId={}, score={}", resume.getId(), job.getId(), hybridScore);
                } catch (Exception e) {
                    rec.put("aiReviewError", e.getMessage());
                    log.warn("AI 增强失败: resumeId={}, jobId={}, error={}", resume.getId(), job.getId(), e.getMessage());
                }
            }));
        }

        // 等待所有 AI 调用完成（最多2分钟）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("AI 并行执行超时或中断: {}", e.getMessage());
        }

        // AI 完成后，读取现有缓存并更新 AI 增强的条目
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cachedList = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedList != null) {
                for (Map<String, Object> aiRec : needsAiReview) {
                    Job aiJob = (Job) aiRec.get("job");
                    if (aiJob == null) continue;
                    for (Map<String, Object> cached : cachedList) {
                        Object jobObj = cached.get("job");
                        Long cachedJobId = null;
                        if (jobObj instanceof Job) {
                            cachedJobId = ((Job) jobObj).getId();
                        } else if (jobObj instanceof Map) {
                            Object idObj = ((Map<?, ?>) jobObj).get("id");
                            if (idObj instanceof Number) cachedJobId = ((Number) idObj).longValue();
                        }
                        if (cachedJobId != null && cachedJobId.equals(aiJob.getId())) {
                            synchronized (cached) {
                                cached.put("matchScore", aiRec.get("matchScore"));
                                cached.put("hybridDetail", aiRec.get("hybridDetail"));
                                cached.put("aiReviewed", true);
                            }
                            break;
                        }
                    }
                }
                redisTemplate.opsForValue().set(cacheKey, cachedList, 30, TimeUnit.MINUTES);
                log.info("AI 增强结果已更新到缓存: resumeId={}", resume.getId());
            }
        } catch (Exception e) {
            log.warn("更新缓存失败: {}", e.getMessage());
        }

        log.info("异步 AI 增强完成: resumeId={}, 处理数量={}", resume.getId(), aiLimit);
    }

    /**
     * 异步 AI 增强 - 雇主端候选人推荐（并行执行，直接更新缓存）
     */
    @Async("aiTaskExecutor")
    public void asyncAiEnhanceForJob(Job job, List<Map<String, Object>> needsAiReview) {
        int aiLimit = Math.min(15, needsAiReview.size());
        String cacheKey = "match:recommend:job:" + job.getId();

        log.info("开始异步 AI 增强(雇主端): jobId={}, 待处理数量={}", job.getId(), aiLimit);

        // 并行执行 AI 调用
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < aiLimit; i++) {
            final Map<String, Object> rec = needsAiReview.get(i);
            final Resume resume = (Resume) rec.get("resume");
            if (resume == null) continue;

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> hybridResult = aiService.hybridMatch(resume, job);
                    double hybridScore = (double) hybridResult.getOrDefault("hybridScore", 0.0);
                    synchronized (rec) {
                        rec.put("matchScore", new BigDecimal(hybridScore).setScale(1, RoundingMode.HALF_UP));
                        rec.put("hybridDetail", hybridResult);
                        rec.put("aiReviewed", true);
                    }
                } catch (Exception e) {
                    rec.put("aiReviewError", e.getMessage());
                    log.warn("AI 增强失败: jobId={}, resumeId={}", job.getId(), resume.getId());
                }
            }));
        }

        // 等待所有 AI 调用完成（最多2分钟）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("AI 并行执行超时或中断: {}", e.getMessage());
        }

        // AI 完成后，读取现有缓存并更新 AI 增强的条目
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cachedList = (List<Map<String, Object>>) cached;
                for (Map<String, Object> aiRec : needsAiReview) {
                    if (!Boolean.TRUE.equals(aiRec.get("aiReviewed"))) continue;
                    Resume aiResume = (Resume) aiRec.get("resume");
                    if (aiResume == null) continue;
                    for (Map<String, Object> cachedRec : cachedList) {
                        Object resObj = cachedRec.get("resume");
                        Long resId = null;
                        if (resObj instanceof Map) {
                            resId = Long.valueOf(((Map<String, Object>) resObj).get("id").toString());
                        } else if (resObj instanceof Resume) {
                            resId = ((Resume) resObj).getId();
                        }
                        if (resId != null && resId.equals(aiResume.getId())) {
                            cachedRec.put("matchScore", aiRec.get("matchScore"));
                            cachedRec.put("hybridDetail", aiRec.get("hybridDetail"));
                            cachedRec.put("aiReviewed", true);
                            break;
                        }
                    }
                }
                // 重新排序
                cachedList.sort((a, b) -> {
                    BigDecimal scoreA = (BigDecimal) a.getOrDefault("matchScore", BigDecimal.ZERO);
                    BigDecimal scoreB = (BigDecimal) b.getOrDefault("matchScore", BigDecimal.ZERO);
                    return scoreB.compareTo(scoreA);
                });
                redisTemplate.opsForValue().set(cacheKey, cachedList, 30, TimeUnit.MINUTES);
                log.info("AI 增强后缓存已更新(雇主端): jobId={}, aiCount={}", job.getId(), aiLimit);
            }
        } catch (Exception e) {
            log.warn("AI 增强后缓存更新失败: {}", e.getMessage());
        }

        log.info("异步 AI 增强完成(雇主端): jobId={}, 处理数量={}", job.getId(), aiLimit);
    }
}
