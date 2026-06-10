package com.smartrecruitment.controller;

import com.smartrecruitment.config.BailianConfig;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * ================================================
 * 测试控制器 - 阿里云百炼 AI 功能测试
 * ================================================
 *
 * URL 前缀：/bailian
 * 用于在开发阶段快速测试百炼 AI 的各项功能
 *
 * 接口列表：
 *   GET  /bailian/test           → 测试百炼基础对话
 *   POST /bailian/test-resume    → 测试简历分析
 *   GET  /bailian/test-match     → 测试匹配度计算
 *
 * 注意：仅用于开发调试，生产环境建议移除或加权限控制
 */
@RestController
@RequestMapping("/bailian")
@CrossOrigin
public class BailianTestController {

    @Autowired
    private BailianConfig bailianConfig;

    @Autowired
    private AIService aiService;

    @GetMapping("/test")
    public ResponseEntity<?> testBailian() {
        try {
            String prompt = "你好，请介绍一下阿里云千问大模型。";
            String result = bailianConfig.call(prompt);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "百炼API测试成功");
            response.put("result", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/test-resume")
    public ResponseEntity<?> testResumeAnalysis(@RequestBody Resume resume) {
        try {
            String analysis = aiService.analyzeResume(resume);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "简历分析测试成功");
            response.put("analysis", analysis);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/test-match")
    public ResponseEntity<?> testMatchScore() {
        try {
            Resume testResume = new Resume();
            testResume.setName("张三");
            testResume.setAge(25);
            testResume.setSkills("Java, Spring Boot, Vue.js, MySQL");
            testResume.setExperience("3年后端开发经验");
            testResume.setEducation("本科");
            testResume.setExpectedSalary(new BigDecimal("15000"));

            com.smartrecruitment.entity.Job testJob = new com.smartrecruitment.entity.Job();
            testJob.setTitle("Java后端工程师");
            testJob.setRequirements("3年以上Java开发经验，熟悉Spring Boot框架");

            BigDecimal score = aiService.calculateMatchScore(testResume, testJob);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "匹配度测试成功");
            response.put("matchScore", score);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
