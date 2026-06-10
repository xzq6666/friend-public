package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartrecruitment.entity.*;
import com.smartrecruitment.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 匹配记录控制器 - v5
 * 新增：屏蔽管理、偏好管理、得分明细、优化建议
 */
@RestController
@RequestMapping("/match")
@CrossOrigin
public class MatchRecordController {

    @Autowired
    private MatchRecordService matchRecordService;
    @Autowired
    private ResumeService resumeService;
    @Autowired
    private UserService userService;
    @Autowired
    private AIService aiService;
    @Autowired
    private MatchBlockService matchBlockService;
    @Autowired
    private JobService jobService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 原有接口 ====================

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<?> getMatchByResume(@PathVariable Long resumeId) {
        try {
            List<Map<String, Object>> records = matchRecordService.getMatchRecordsWithJob(resumeId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getMatchByJob(@PathVariable Long jobId) {
        try {
            List<Map<String, Object>> records = matchRecordService.getMatchRecordsWithResume(jobId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/page")
    public ResponseEntity<?> getMatchPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long jobId) {
        try {
            IPage<MatchRecord> matchPage = matchRecordService.getMatchRecordPage(page, size, resumeId, jobId);
            return ResponseEntity.ok(matchPage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 为当前用户推荐职位（v5：过滤屏蔽项）
     */
    @GetMapping("/recommend/jobs")
    public ResponseEntity<?> recommendJobs(
            Authentication auth,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));
            }

            // 获取用户屏蔽的职位列表
            Set<Long> blockedJobIds = matchBlockService.getBlockedJobIds(user.getId());

            List<Map<String, Object>> recommendations = matchRecordService.recommendJobsForResume(resume.getId(), limit * 2);
            // 过滤屏蔽项 - 支持LinkedHashMap和Job实体两种类型
            recommendations.removeIf(r -> {
                Object jobObj = r.get("job");
                Long jobId = null;
                if (jobObj instanceof Job) {
                    jobId = ((Job) jobObj).getId();
                } else if (jobObj instanceof Map) {
                    Object idObj = ((Map<?, ?>) jobObj).get("id");
                    if (idObj instanceof Number) {
                        jobId = ((Number) idObj).longValue();
                    } else if (idObj != null) {
                        jobId = Long.valueOf(idObj.toString());
                    }
                }
                return jobId != null && blockedJobIds.contains(jobId);
            });
            if (recommendations.size() > limit) {
                recommendations = recommendations.subList(0, limit);
            }
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 为职位推荐候选人（v5：过滤屏蔽项）
     */
    @GetMapping("/recommend/candidates/{jobId}")
    public ResponseEntity<?> recommendCandidates(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> recommendations = matchRecordService.recommendCandidatesForJob(jobId, limit);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateMatchStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer status = body.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "状态值不能为空"));
            }
            boolean success = matchRecordService.updateMatchStatus(id, status);
            if (success) return ResponseEntity.ok(Map.of("message", "状态更新成功"));
            else return ResponseEntity.badRequest().body(Map.of("error", "状态更新失败"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMatchRecord(@PathVariable Long id) {
        try {
            boolean success = matchRecordService.removeById(id);
            if (success) return ResponseEntity.ok(Map.of("message", "删除成功"));
            else return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== 匹配得分明细 ====================

    /**
     * 获取简历与职位的匹配得分明细（含对照、建议）
     */
    @GetMapping("/detail")
    public ResponseEntity<?> getMatchDetail(
            @RequestParam Long resumeId,
            @RequestParam Long jobId) {
        try {
            Resume resume = resumeService.getById(resumeId);
            Job job = jobService.findById(jobId);
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            Map<String, Object> detail = matchRecordService.calculateMatchDetail(resume, job);
            detail.put("resumeId", resumeId);
            detail.put("jobId", jobId);
            detail.put("jobTitle", job.getTitle());
            detail.put("resumeName", resume.getName());
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取匹配明细失败: " + e.getMessage()));
        }
    }

    /**
     * 获取简历优化建议（基于匹配短板）
     */
    @GetMapping("/suggestions")
    public ResponseEntity<?> getOptimizationSuggestions(
            @RequestParam Long resumeId,
            @RequestParam Long jobId) {
        try {
            Resume resume = resumeService.getById(resumeId);
            Job job = jobService.findById(jobId);
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            Map<String, Object> detail = matchRecordService.calculateMatchDetail(resume, job);
            @SuppressWarnings("unchecked")
            List<String> suggestions = (List<String>) detail.get("suggestions");
            return ResponseEntity.ok(Map.of(
                "resumeId", resumeId,
                "jobId", jobId,
                "suggestions", suggestions != null ? suggestions : Collections.emptyList(),
                "matchScore", detail.get("total")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取建议失败: " + e.getMessage()));
        }
    }

    // ==================== 屏蔽管理 ====================

    /**
     * 添加屏蔽
     * body: { blockType: 1|2, jobId?: Long, resumeId?: Long, reason?: String }
     */
    @PostMapping("/block")
    public ResponseEntity<?> addBlock(Authentication auth, @RequestBody Map<String, Object> body) {
        try {
            if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Integer blockType = (Integer) body.get("blockType");
            Long jobId = body.get("jobId") != null ? Long.valueOf(body.get("jobId").toString()) : null;
            Long resumeId = body.get("resumeId") != null ? Long.valueOf(body.get("resumeId").toString()) : null;
            String reason = (String) body.get("reason");

            boolean success = matchBlockService.addBlock(user.getId(), blockType, jobId, resumeId, reason);
            if (success) return ResponseEntity.ok(Map.of("message", "已屏蔽"));
            else return ResponseEntity.badRequest().body(Map.of("error", "屏蔽失败"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "操作失败: " + e.getMessage()));
        }
    }

    /**
     * 取消屏蔽
     */
    @DeleteMapping("/block")
    public ResponseEntity<?> removeBlock(Authentication auth,
                                          @RequestParam Integer blockType,
                                          @RequestParam Long targetId) {
        try {
            if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            boolean success = matchBlockService.removeBlock(user.getId(), blockType, targetId);
            if (success) return ResponseEntity.ok(Map.of("message", "已取消屏蔽"));
            else return ResponseEntity.badRequest().body(Map.of("error", "操作失败"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "操作失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户的屏蔽列表
     */
    @GetMapping("/blocks")
    public ResponseEntity<?> getUserBlocks(Authentication auth,
                                            @RequestParam(required = false) Integer blockType) {
        try {
            if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            List<MatchBlock> blocks = matchBlockService.getUserBlocks(user.getId(), blockType);
            List<Map<String, Object>> result = new ArrayList<>();
            for (MatchBlock block : blocks) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", block.getId());
                item.put("blockType", block.getBlockType());
                item.put("jobId", block.getJobId());
                item.put("resumeId", block.getResumeId());
                item.put("reason", block.getReason());
                item.put("createTime", block.getCreateTime());
                if (block.getJobId() != null) {
                    Job job = jobService.getById(block.getJobId());
                    item.put("jobTitle", job != null ? job.getTitle() : null);
                }
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 检查是否已屏蔽
     */
    @GetMapping("/block/check")
    public ResponseEntity<?> checkBlocked(Authentication auth,
                                           @RequestParam Integer blockType,
                                           @RequestParam Long targetId) {
        try {
            if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            boolean blocked = matchBlockService.isBlocked(user.getId(), blockType, targetId);
            return ResponseEntity.ok(Map.of("blocked", blocked));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== AI 语义匹配（保留）====================

    @GetMapping("/ai-match")
    public ResponseEntity<?> aiSemanticMatch(
            @RequestParam Long resumeId,
            @RequestParam Long jobId) {
        try {
            Resume resume = resumeService.getById(resumeId);
            Job job = jobService.findById(jobId);
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            BigDecimal score = aiService.calculateSemanticMatchScore(resume, job);
            return ResponseEntity.ok(Map.of(
                "resumeId", resumeId, "jobId", jobId,
                "aiSemanticScore", score, "method", "AI语义匹配"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "AI语义匹配失败: " + e.getMessage()));
        }
    }

    @GetMapping("/hybrid-match")
    public ResponseEntity<?> hybridMatch(
            @RequestParam Long resumeId,
            @RequestParam Long jobId) {
        try {
            Resume resume = resumeService.getById(resumeId);
            Job job = jobService.findById(jobId);
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            Map<String, Object> result = aiService.hybridMatch(resume, job);
            result.put("resumeId", resumeId);
            result.put("jobId", jobId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "混合匹配失败: " + e.getMessage()));
        }
    }

    @GetMapping("/explanation")
    public ResponseEntity<?> getMatchExplanation(
            @RequestParam Long resumeId,
            @RequestParam Long jobId) {
        try {
            Resume resume = resumeService.getById(resumeId);
            Job job = jobService.findById(jobId);
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            Map<String, Object> ruleDetail = matchRecordService.calculateMatchDetail(resume, job);
            String explanation = aiService.generateMatchExplanation(resume, job, ruleDetail);
            return ResponseEntity.ok(Map.of(
                "resumeId", resumeId, "jobId", jobId, "explanation", explanation
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "生成解释失败: " + e.getMessage()));
        }
    }

    // ==================== 缓存管理 ====================

    /**
     * 清除当前用户的职位推荐缓存（强制重新匹配）
     */
    @DeleteMapping("/cache/jobs")
    public ResponseEntity<?> clearJobRecommendCache(Authentication auth) {
        try {
            if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));

            redisTemplate.delete("match:recommend:resume:" + resume.getId());
            return ResponseEntity.ok(Map.of("message", "推荐缓存已清除，下次匹配将重新计算"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "清除缓存失败: " + e.getMessage()));
        }
    }

    /**
     * 清除指定职位的候选人推荐缓存（强制重新匹配）
     */
    @DeleteMapping("/cache/candidates/{jobId}")
    public ResponseEntity<?> clearCandidateRecommendCache(@PathVariable Long jobId) {
        try {
            redisTemplate.delete("match:recommend:job:" + jobId);
            return ResponseEntity.ok(Map.of("message", "候选人推荐缓存已清除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "清除缓存失败: " + e.getMessage()));
        }
    }
}
