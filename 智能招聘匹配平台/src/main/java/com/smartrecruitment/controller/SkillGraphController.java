package com.smartrecruitment.controller;

import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.SkillGraph;
import com.smartrecruitment.entity.SkillRelation;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.SkillGraphService;
import com.smartrecruitment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能图谱控制器
 */
@RestController
@RequestMapping("/skill-graph")
@CrossOrigin
public class SkillGraphController {

    @Autowired
    private SkillGraphService skillGraphService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;

    /**
     * 初始化技能图谱数据
     */
    @PostMapping("/init")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initSkillGraph() {
        try {
            skillGraphService.initSkillGraphData();
            return ResponseEntity.ok(Map.of("message", "技能图谱数据初始化成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 从系统真实数据自动构建技能图谱（自动添加新技能到数据库）
     */
    @PostMapping("/auto-build")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> autoBuild() {
        try {
            Map<String, Object> result = skillGraphService.autoBuildFromRealData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 同步所有简历和职位的技能到图谱
     */
    @PostMapping("/sync-skills")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncSkills() {
        try {
            // 调用 autoBuild 会自动提取并添加新技能
            Map<String, Object> result = skillGraphService.autoBuildFromRealData();
            return ResponseEntity.ok(Map.of(
                "message", "技能同步完成",
                "newSkillsAdded", result.get("newSkillsAdded"),
                "newFromResumes", result.get("newFromResumes"),
                "newFromJobs", result.get("newFromJobs"),
                "totalSkills", skillGraphService.count()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有技能节点
     */
    @GetMapping("/skills")
    public ResponseEntity<?> getAllSkills() {
        try {
            List<SkillGraph> skills = skillGraphService.getAllSkills();
            return ResponseEntity.ok(skills);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有技能关系
     */
    @GetMapping("/relations")
    public ResponseEntity<?> getAllRelations() {
        try {
            List<SkillRelation> relations = skillGraphService.getAllRelations();
            return ResponseEntity.ok(relations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有行业列表
     */
    @GetMapping("/industries")
    public ResponseEntity<?> getIndustries() {
        try {
            return ResponseEntity.ok(skillGraphService.getAllIndustries());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 根据行业获取技能
     */
    @GetMapping("/skills/industry/{industry}")
    public ResponseEntity<?> getSkillsByIndustry(@PathVariable String industry) {
        try {
            return ResponseEntity.ok(skillGraphService.getSkillsByIndustry(industry));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 根据分类获取技能
     */
    @GetMapping("/skills/category/{category}")
    public ResponseEntity<?> getSkillsByCategory(@PathVariable String category) {
        try {
            List<SkillGraph> skills = skillGraphService.getSkillsByCategory(category);
            return ResponseEntity.ok(skills);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取个人能力图谱数据
     */
    @GetMapping("/personal")
    public ResponseEntity<?> getPersonalGraph(Authentication auth,
                                               @RequestParam(required = false) String industry) {
        try {
            String username = auth.getName();
            com.smartrecruitment.entity.User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            Resume resume = resumeService.getResumeByUserId(user.getId());
            if (resume == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "请先创建简历"));
            }

            Map<String, Object> graphData;
            if (industry != null && !industry.isEmpty()) {
                graphData = skillGraphService.buildPersonalGraphByIndustry(resume.getSkills(), industry);
            } else {
                graphData = skillGraphService.buildPersonalGraph(resume.getSkills());
            }
            return ResponseEntity.ok(graphData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 根据简历ID获取能力图谱
     */
    @GetMapping("/personal/{resumeId}")
    public ResponseEntity<?> getPersonalGraphByResumeId(@PathVariable Long resumeId) {
        try {
            Resume resume = resumeService.getById(resumeId);
            if (resume == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            }

            Map<String, Object> graphData = skillGraphService.buildPersonalGraph(resume.getSkills());
            return ResponseEntity.ok(graphData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取职位能力图谱数据
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getJobGraph(@PathVariable Long jobId,
                                          @RequestParam(required = false) String industry) {
        try {
            Job job = jobService.findById(jobId);
            if (job == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "职位不存在"));
            }

            String requirements = job.getDescription() + " " + job.getRequirements();
            Map<String, Object> graphData;
            if (industry != null && !industry.isEmpty()) {
                graphData = skillGraphService.buildJobGraphByIndustry(requirements, industry);
            } else {
                graphData = skillGraphService.buildJobGraph(requirements);
            }
            return ResponseEntity.ok(graphData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 添加自定义技能节点
     */
    @PostMapping("/skill")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addSkill(@RequestBody SkillGraph skill) {
        try {
            SkillGraph addedSkill = skillGraphService.addSkill(skill);
            return ResponseEntity.ok(addedSkill);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 添加技能关系
     */
    @PostMapping("/relation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addRelation(@RequestBody SkillRelation relation) {
        try {
            SkillRelation addedRelation = skillGraphService.addRelation(relation);
            return ResponseEntity.ok(addedRelation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取技能图谱统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSkills", skillGraphService.count());
            stats.put("totalRelations", skillGraphService.getAllRelations().size());
            stats.put("categories", skillGraphService.getAllSkills().stream()
                    .map(SkillGraph::getCategory)
                    .distinct()
                    .count());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 技能推荐：根据当前技能和目标职位要求推荐学习技能
     */
    @GetMapping("/recommend")
    public ResponseEntity<?> recommendSkills(
            @RequestParam String currentSkills,
            @RequestParam String targetRequirements) {
        try {
            List<Map<String, Object>> recommendations = skillGraphService.recommendSkills(currentSkills, targetRequirements);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 技能差距分析：对比当前技能与目标职位要求
     */
    @GetMapping("/gap-analysis")
    public ResponseEntity<?> analyzeSkillGap(
            @RequestParam String currentSkills,
            @RequestParam String targetRequirements) {
        try {
            Map<String, Object> result = skillGraphService.analyzeSkillGap(currentSkills, targetRequirements);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取技能学习路径
     */
    @GetMapping("/learning-path")
    public ResponseEntity<?> getLearningPath(
            @RequestParam String fromSkill,
            @RequestParam String toSkill) {
        try {
            List<SkillGraph> path = skillGraphService.getLearningPath(fromSkill, toSkill);
            if (path.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "未找到从 " + fromSkill + " 到 " + toSkill + " 的学习路径", "path", path));
            }
            return ResponseEntity.ok(Map.of("path", path));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 诊断接口 - 检查数据库表状态
     */
    @GetMapping("/diagnose")
    public ResponseEntity<?> diagnose() {
        Map<String, Object> result = new HashMap<>();
        try {
            long skillCount = skillGraphService.count();
            result.put("skillTableExists", true);
            result.put("skillCount", skillCount);
        } catch (Exception e) {
            result.put("skillTableExists", false);
            result.put("skillTableError", e.getMessage());
        }
        try {
            int relationCount = skillGraphService.getAllRelations().size();
            result.put("relationTableExists", true);
            result.put("relationCount", relationCount);
        } catch (Exception e) {
            result.put("relationTableExists", false);
            result.put("relationTableError", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
