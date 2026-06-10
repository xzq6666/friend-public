package com.smartrecruitment.controller;

import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.AIService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI高级功能控制器
 *
 * 接口列表：
 *   GET  /ai/cover-letter          → 生成求职信
 *   GET  /ai/optimize-resume        → 简历优化建议
 *   GET  /ai/predict-salary         → 薪资预测
 *   GET  /ai/career-path            → 职业发展路径
 *   POST /ai/compare-resumes        → 简历对比
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AIAdvancedController {

    @Autowired
    private AIService aiService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.smartrecruitment.service.JobService jobService;

    /**
     * 生成求职信
     */
    @GetMapping("/cover-letter")
    public ResponseEntity<?> generateCoverLetter(
            Authentication auth,
            @RequestParam Long jobId) {
        try {
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));

            // 获取职位信息
            com.smartrecruitment.entity.Job job = getJobById(jobId);
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            String coverLetter = aiService.generateCoverLetter(resume, job);
            return ResponseEntity.ok(Map.of("coverLetter", coverLetter));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "生成求职信失败: " + e.getMessage()));
        }
    }

    /**
     * 简历优化建议
     */
    @GetMapping("/optimize-resume")
    public ResponseEntity<?> optimizeResume(
            Authentication auth,
            @RequestParam Long jobId) {
        try {
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));

            com.smartrecruitment.entity.Job job = getJobById(jobId);
            if (job == null) return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));

            String suggestions = aiService.optimizeResume(resume, job);
            return ResponseEntity.ok(Map.of("suggestions", suggestions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "简历优化失败: " + e.getMessage()));
        }
    }

    /**
     * 薪资预测
     */
    @GetMapping("/predict-salary")
    public ResponseEntity<?> predictSalary(Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));

            Map<String, Object> prediction = aiService.predictSalary(resume);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "薪资预测失败: " + e.getMessage()));
        }
    }

    /**
     * 职业发展路径
     */
    @GetMapping("/career-path")
    public ResponseEntity<?> generateCareerPath(Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));

            String careerPath = aiService.generateCareerPath(resume);
            return ResponseEntity.ok(Map.of("careerPath", careerPath));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "职业路径生成失败: " + e.getMessage()));
        }
    }

    /**
     * 简历对比
     */
    @PostMapping("/compare-resumes")
    public ResponseEntity<?> compareResumes(@RequestBody Map<String, Long> body) {
        try {
            Long resumeId1 = body.get("resumeId1");
            Long resumeId2 = body.get("resumeId2");

            if (resumeId1 == null || resumeId2 == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "请提供两份简历的ID"));
            }

            Resume r1 = resumeService.getById(resumeId1);
            Resume r2 = resumeService.getById(resumeId2);

            if (r1 == null || r2 == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            }

            String comparison = aiService.compareResumes(r1, r2);
            return ResponseEntity.ok(Map.of("comparison", comparison));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "简历对比失败: " + e.getMessage()));
        }
    }

    private com.smartrecruitment.entity.Job getJobById(Long jobId) {
        return jobService.findById(jobId);
    }
}
