package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobApplication;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.DocumentParseService;
import com.smartrecruitment.service.FavoriteService;
import com.smartrecruitment.service.FileUploadService;
import com.smartrecruitment.service.JobApplicationService;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.OperationLogService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.service.VisitHistoryService;
import com.smartrecruitment.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ================================================
 * 简历管理控制器
 * ================================================
 *
 * URL 前缀：/resume（需登录认证）
 *
 * 接口列表：
 *   POST   /resume             → 创建简历（自动 AI 分析）
 *   GET    /resume/page        → 简历列表（分页+关键词搜索）
 *   GET    /resume/{id}        → 获取简历详情
 *   PUT    /resume/{id}        → 更新简历
 *   DELETE /resume/{id}        → 删除简历
 *   POST   /resume/{id}/ai-analyze → AI 分析简历
 *
 * 如需扩展：
 *   - 添加 POST /resume/upload 简历文件上传（PDF/Word/图片）
 *   - 添加 GET /resume/export/{id} 导出简历
 *   - 添加 POST /resume/batch-delete 批量删除
 *   - 添加 GET /resume/user/{userId} 获取某用户的所有简历
 *   - 添加高级搜索（按技能/薪资/学历等多维度筛选）
 *   - 添加简历查重功能
 */
@RestController
@RequestMapping("/resume")
@CrossOrigin
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserService userService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private DocumentParseService documentParseService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private com.smartrecruitment.service.SkillGraphService skillGraphService;

    @Autowired
    private VisitHistoryService visitHistoryService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyResume(Authentication auth) {
        String username = auth.getName();
        com.smartrecruitment.entity.User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // 返回用户的默认简历
        Resume resume = resumeService.getDefaultResumeByUserId(user.getId());
        if (resume != null) {
            return ResponseEntity.ok(resume);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取用户的所有简历列表
     */
    @GetMapping("/my/list")
    public ResponseEntity<?> getMyResumeList(Authentication auth) {
        String username = auth.getName();
        com.smartrecruitment.entity.User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        List<Resume> resumes = resumeService.getResumesByUserId(user.getId());
        return ResponseEntity.ok(resumes);
    }

    /**
     * 通过用户ID获取默认简历（公开主页用）
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getResumeByUserId(@PathVariable Long userId, Authentication auth) {
        Resume resume = resumeService.getDefaultResumeByUserId(userId);
        if (resume == null) {
            return ResponseEntity.notFound().build();
        }
        // 记录访问历史
        if (auth != null && !"anonymousUser".equals(auth.getName())) {
            User visitor = userService.findByUsername(auth.getName());
            if (visitor != null && !visitor.getId().equals(userId)) {
                visitHistoryService.recordVisit(visitor.getId(), visitor.getUsername(),
                        visitor.getUserType(), visitor.getAvatar(), 1, resume.getId(), userId, null);
            }
        }
        // 隐藏敏感信息
        resume.setPhone(maskPhone(resume.getPhone()));
        resume.setEmail(maskEmail(resume.getEmail()));
        return ResponseEntity.ok(resume);
    }

    /**
     * 根据ID获取简历（用于投递时选择）
     */
    @GetMapping("/my/{id}")
    public ResponseEntity<?> getMyResumeById(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        com.smartrecruitment.entity.User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Resume resume = resumeService.getById(id);
        if (resume != null && resume.getUserId().equals(user.getId())) {
            return ResponseEntity.ok(resume);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createResume(@RequestBody Resume resume, Authentication auth) {
        try {
            String username = auth.getName();
            com.smartrecruitment.entity.User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            resume.setUserId(user.getId());
            
            // 如果这是用户的第一份简历，设为默认
            List<Resume> existingResumes = resumeService.getResumesByUserId(user.getId());
            if (existingResumes.isEmpty()) {
                resume.setIsDefault(1);
            } else {
                resume.setIsDefault(0);
            }
            
            // 如果没有设置简历名称，使用默认名称
            if (resume.getResumeName() == null || resume.getResumeName().isEmpty()) {
                resume.setResumeName((resume.getName() != null ? resume.getName() : "我的") + "的简历");
            }
            
            Resume createdResume = resumeService.createResume(resume);

            // 同步技能到图谱
            final String sg = createdResume.getSkills();
            final Long catId = createdResume.getCategoryId();
            new Thread(() -> {
                try {
                    int added = skillGraphService.syncSkillsToGraph(sg, catId);
                    if (added > 0) log.info("技能图谱自动新增{}个技能", added);
                } catch (Exception e) { log.warn("技能图谱同步失败: {}", e.getMessage()); }
            }).start();

            // 记录日志
            operationLogService.log(
                    user.getId(),
                    user.getUsername(),
                    "创建",
                    "简历",
                    String.format("创建了简历 [%s]", createdResume.getResumeName() != null ? createdResume.getResumeName() : "未命名"),
                    IpUtils.getClientIp(),
                    1
            );
            return ResponseEntity.ok(createdResume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 分页
     * @param page
     * @param size
     * @param keyword
     * @param auth
     * @return
     */
    @GetMapping("/page")
    public ResponseEntity<?> pageResumes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        try {
            User user = null;
            if (auth != null && !"anonymousUser".equals(auth.getName())) {
                String username = auth.getName();
                user = userService.findByUsername(username);
            }

            IPage<Resume> pageResult = new Page<>(page, size);
            LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
            
            if (user != null && "EMPLOYER".equals(user.getUserType())) {
                // 企业用户只能查看投递了自己职位的求职者简历
                List<Job> myJobs = jobService.lambdaQuery().eq(Job::getEmployerId, user.getId()).list();
                if (myJobs.isEmpty()) {
                    return ResponseEntity.ok(new Page<>(page, size));
                }
                List<Long> jobIds = myJobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
                
                // 查找投递了这些职位的用户ID
                List<JobApplication> applications = jobApplicationService.lambdaQuery()
                        .in(JobApplication::getJobId, jobIds)
                        .select(JobApplication::getUserId)
                        .list();
                List<Long> applicantIds = applications.stream()
                        .map(JobApplication::getUserId)
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());

                if (applicantIds.isEmpty()) {
                    return ResponseEntity.ok(new Page<>(page, size));
                }
                wrapper.in(Resume::getUserId, applicantIds);
            }

            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(Resume::getName, keyword).or().like(Resume::getSkills, keyword));
            }
            wrapper.orderByDesc(Resume::getCreateTime);
            pageResult = resumeService.page(pageResult, wrapper);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 人才市场 - 查看所有简历（企业和管理员专用）
     */
    @GetMapping("/talent-market")
    public ResponseEntity<?> talentMarketPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String education,
            Authentication auth) {
        try {
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            IPage<Resume> pageResult = new Page<>(page, size);
            LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();

            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(Resume::getName, keyword).or().like(Resume::getSkills, keyword));
            }
            if (education != null && !education.isEmpty()) {
                wrapper.eq(Resume::getEducation, education);
            }
            wrapper.orderByDesc(Resume::getCreateTime);
            pageResult = resumeService.page(pageResult, wrapper);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResume(@PathVariable Long id, Authentication auth) {
        try {
            Resume resume = resumeService.getById(id);
            if (resume == null) {
                return ResponseEntity.notFound().build();
            }
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            User currentUser = userService.findByUsername(auth.getName());
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
            }

            boolean isOwner = currentUser.getId().equals(resume.getUserId());
            boolean isAdmin = "ADMIN".equals(currentUser.getUserType());

            // 非所有者且非管理员时记录访问历史
            if (!isOwner && !isAdmin) {
                visitHistoryService.recordVisit(currentUser.getId(), currentUser.getUsername(),
                        currentUser.getUserType(), currentUser.getAvatar(), 1, resume.getId(), resume.getUserId(), null);
                // 企业查看简历时通知求职者
                if ("EMPLOYER".equals(currentUser.getUserType())) {
                    notificationService.send(
                            resume.getUserId(),
                            "简历被查看",
                            String.format("企业用户 %s 查看了您的简历", currentUser.getUsername()),
                            "job_match"
                    );
                }
            }

            // 所有者和管理员查看完整信息
            if (isOwner || isAdmin) {
                return ResponseEntity.ok(resume);
            }

            // 其他用户查看时隐藏敏感信息
            resume.setPhone(maskPhone(resume.getPhone()));
            resume.setEmail(maskEmail(resume.getEmail()));
            return ResponseEntity.ok(resume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResume(@PathVariable Long id, @RequestBody Resume resume, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            // 验证所有权
            Resume existing = resumeService.getById(id);
            if (existing == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            }
            if (!currentUser.getId().equals(existing.getUserId()) && !"ADMIN".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权修改此简历"));
            }
            resume.setId(id);
            boolean success = resumeService.updateById(resume);
            if (success) {
                // 同步技能到图谱
                final Resume savedResume = resumeService.getById(id);
                new Thread(() -> {
                    try {
                        int added = skillGraphService.syncSkillsToGraph(savedResume.getSkills(), savedResume.getCategoryId());
                        if (added > 0) log.info("技能图谱自动新增{}个技能", added);
                    } catch (Exception e) { log.warn("技能图谱同步失败: {}", e.getMessage()); }
                }).start();

                // 记录日志
                if (currentUser != null) {
                    operationLogService.log(
                            currentUser.getId(),
                            currentUser.getUsername(),
                            "更新",
                            "简历",
                            String.format("更新了简历 [%s]", resume.getName() != null ? resume.getName() : "ID:" + id),
                            IpUtils.getClientIp(),
                            1
                    );
                }
                return ResponseEntity.ok(resume);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "更新失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            // 获取简历
            Resume oldResume = resumeService.getById(id);
            if (oldResume == null) {
                log.warn("删除简历失败：简历ID {} 不存在", id);
                return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            }
            
            // 验证权限
            if (!currentUser.getId().equals(oldResume.getUserId()) && !"ADMIN".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body(Map.of("error", "无权删除此简历"));
            }
            
            // 执行删除
            boolean success = resumeService.removeById(id);
            if (success) {
                // 记录日志
                operationLogService.log(
                        currentUser.getId(),
                        currentUser.getUsername(),
                        "删除",
                        "简历",
                        String.format("删除了简历 [%s]", oldResume.getResumeName() != null ? oldResume.getResumeName() : (oldResume.getName() != null ? oldResume.getName() : "ID:" + id)),
                        IpUtils.getClientIp(),
                        1
                );
                log.info("用户 {} 成功删除简历 ID: {}", currentUser.getUsername(), id);
                return ResponseEntity.ok(Map.of("message", "删除成功"));
            } else {
                log.error("删除简历失败：ID {}", id);
                return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
            }
        } catch (Exception e) {
            log.error("删除简历异常", e);
            return ResponseEntity.badRequest().body(Map.of("error", "删除异常: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resumeId", required = false) Long resumeId,
            Authentication auth) {
        try {
            String username = auth.getName();
            com.smartrecruitment.entity.User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            // 1. 上传文件到磁盘存储
            String relativePath = fileUploadService.upload(file);

            // 2. 使用AI智能解析简历（包含工作经历、教育背景、自我评价等完整信息）
            Map<String, Object> parsedData = documentParseService.parseResumeDocument(file);
            String text = (String) parsedData.getOrDefault("rawText", "");
            log.info("[上传简历] AI解析完成，提取到{}个字段，parseMethod={}", parsedData.size(), parsedData.get("parseMethod"));

            // 3. 将解析结果映射到Resume实体
            Resume basicResume = new Resume();
            basicResume.setUserId(user.getId());
            basicResume.setFileUrl(relativePath);
            basicResume.setName((String) parsedData.get("name"));
            basicResume.setPhone((String) parsedData.get("phone"));
            basicResume.setEmail((String) parsedData.get("email"));
            basicResume.setAge(parseAgeFromObj(parsedData.get("age")));
            basicResume.setEducation((String) parsedData.get("education"));
            basicResume.setExperience((String) parsedData.get("experience"));
            basicResume.setSelfIntroduction((String) parsedData.get("selfIntroduction"));
            basicResume.setAiAnalysis((String) parsedData.get("aiAnalysis"));

            // 处理skills字段 - 确保是JSON数组格式
            String skills = (String) parsedData.get("skills");
            basicResume.setSkills(ensureJsonArray(skills));

            // 处理workExperience字段 - 确保是JSON数组格式
            String workExp = (String) parsedData.get("workExperience");
            basicResume.setWorkExperience(ensureJsonArray(workExp));

            // 处理薪资
            Object salaryObj = parsedData.get("expectedSalary");
            if (salaryObj instanceof java.math.BigDecimal) {
                basicResume.setExpectedSalary((java.math.BigDecimal) salaryObj);
            } else if (salaryObj instanceof String) {
                try {
                    String salaryStr = ((String) salaryObj).replaceAll("[^0-9.]", "");
                    if (!salaryStr.isEmpty()) {
                        basicResume.setExpectedSalary(new java.math.BigDecimal(salaryStr));
                    }
                } catch (NumberFormatException ignored) {}
            }
            
            Resume resume;
            String operationType;
            
            // 4. 如果传入了resumeId，则更新指定简历；否则查找或创建
            if (resumeId != null) {
                // 更新指定简历
                Resume existing = resumeService.getById(resumeId);
                if (existing == null || !existing.getUserId().equals(user.getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "简历不存在或无权限"));
                }
                resume = existing;
                operationType = "更新";
                
                // 对于上传文件，完全覆盖简历内容（不只是更新非null字段）
                resume.setName(basicResume.getName());
                resume.setPhone(basicResume.getPhone());
                resume.setEmail(basicResume.getEmail());
                resume.setAge(basicResume.getAge());
                resume.setEducation(basicResume.getEducation());
                resume.setSkills(basicResume.getSkills());
                resume.setExperience(basicResume.getExperience());
                resume.setWorkExperience(basicResume.getWorkExperience());
                resume.setSelfIntroduction(basicResume.getSelfIntroduction());
                resume.setFileUrl(basicResume.getFileUrl());
                
                log.info("[上传简历] 传入 resumeId={}, 更新指定简历", resumeId);
            } else {
                // 查找该用户已有的简历，如果有则更新，否则创建
                resume = resumeService.getResumeByUserId(user.getId());
                if (resume != null) {
                    operationType = "更新";
                    // 更新逻辑：只更新非null字段（保持向后兼容）
                    if (basicResume.getName() != null) resume.setName(basicResume.getName());
                    if (basicResume.getPhone() != null) resume.setPhone(basicResume.getPhone());
                    if (basicResume.getEmail() != null) resume.setEmail(basicResume.getEmail());
                    if (basicResume.getAge() != null) resume.setAge(basicResume.getAge());
                    if (basicResume.getEducation() != null) resume.setEducation(basicResume.getEducation());
                    if (basicResume.getSkills() != null) resume.setSkills(basicResume.getSkills());
                    if (basicResume.getExperience() != null) resume.setExperience(basicResume.getExperience());
                    if (basicResume.getExpectedSalary() != null) resume.setExpectedSalary(basicResume.getExpectedSalary());
                    if (basicResume.getWorkExperience() != null) resume.setWorkExperience(basicResume.getWorkExperience());
                    if (basicResume.getSelfIntroduction() != null) resume.setSelfIntroduction(basicResume.getSelfIntroduction());
                    resume.setFileUrl(basicResume.getFileUrl());
                } else {
                    operationType = "创建";
                    resume = basicResume;
                    resume.setUserId(user.getId());
                }
            }
            
            // 保存或更新
            if (operationType.equals("创建")) {
                // 如果是第一份简历，设为默认
                List<Resume> existingResumes = resumeService.getResumesByUserId(user.getId());
                if (existingResumes.isEmpty()) {
                    resume.setIsDefault(1);
                } else {
                    resume.setIsDefault(0);
                }
                resumeService.createResume(resume);
            } else {
                resumeService.updateById(resume);
            }

            // 同步技能到图谱（异步，不阻塞返回）
            final String skillsForGraph = resume.getSkills();
            final Long catId = resume.getCategoryId();
            new Thread(() -> {
                try {
                    int added = skillGraphService.syncSkillsToGraph(skillsForGraph, catId);
                    if (added > 0) log.info("技能图谱自动新增{}个技能", added);
                } catch (Exception e) {
                    log.warn("技能图谱同步失败: {}", e.getMessage());
                }
            }).start();

            // 记录日志
            operationLogService.log(
                    user.getId(),
                    user.getUsername(),
                    operationType,
                    "简历",
                    String.format("%s了简历文件 [%s]", operationType, file.getOriginalFilename()),
                    IpUtils.getClientIp(),
                    1
            );

            // 5. 构建返回数据
            Map<String, Object> response = new HashMap<>();
            response.put("id", resume.getId());
            response.put("name", resume.getName());
            response.put("phone", resume.getPhone());
            response.put("email", resume.getEmail());
            response.put("age", resume.getAge());
            response.put("education", resume.getEducation());
            response.put("skills", resume.getSkills());
            response.put("experience", resume.getExperience());
            response.put("workExperience", resume.getWorkExperience());
            response.put("selfIntroduction", resume.getSelfIntroduction());
            response.put("aiAnalysis", resume.getAiAnalysis());
            response.put("fileUrl", resume.getFileUrl());
            response.put("message", "简历上传并解析成功");
            
            log.info("[上传简历] 返回数据 - name: {}, workExperience: {}, skills: {}", 
                resume.getName(), resume.getWorkExperience(), resume.getSkills());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("上传简历失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 正则快速提取方法
    private String extractPhone(String text) {
        // 格式: 电话:13800138000 / 手机:138-0013-8000 / 138 0013 8000
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:电话|手机|联系电话|联系方式|Tel|Phone)[：:]\\s*([\\d\\s\\-]{7,20})").matcher(text);
        if (m.find()) {
            String phone = m.group(1).replaceAll("[\\s\\-]", "");
            if (phone.matches("1[3-9]\\d{9}")) return phone;
        }
        // 直接匹配11位手机号
        m = java.util.regex.Pattern.compile("(1[3-9]\\d{9})").matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private String extractEmail(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:邮箱|Email|E-mail|电子邮)[：:]\\s*([\\w\\.\\-]+@[\\w\\.\\-]+\\.[a-zA-Z]{2,})").matcher(text);
        if (m.find()) return m.group(1);
        // 直接匹配邮箱
        m = java.util.regex.Pattern.compile("([\\w\\.\\-]+@[\\w\\.\\-]+\\.[a-zA-Z]{2,})").matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private String extractName(String text) {
        // 扩展黑名单，覆盖常见非姓名词汇（表格表头、常见字段等）
        String[] blacklist = {"性别", "男", "女", "年龄", "邮箱", "电话", "手机", "学历", "教育", "技能", "经验", "工作", "项目", "个人", "简历", "意向", "期望", "求职", "城市", "岗位", "职位", "地址", "籍贯", "政治", "婚姻", "身高", "体重", "兴趣", "爱好", "特长", "证书", "本科", "硕士", "博士", "大专", "专科", "研究生", "高中", "初中", "小学", "大学", "学院", "学校", "专业", "至今", "现在", "基本信息"};
    
        // 策略1: 键值对匹配 (姓名: 张三 / 姓名  张三)
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<!.)姓名[\\s：:]+([\\u4e00-\\u9fa5\\u00b7]{2,4})(?![\\u4e00-\\u9fa5])").matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!isBlacklisted(name, blacklist) && !name.equals("姓名")) return name;
        }
            
        // 策略2: 表格格式匹配 (寻找"姓名"后的非关键词文本，支持跨行)
        m = java.util.regex.Pattern.compile("(?<!.)姓名[\\s\\n\\r\\t]*[：:]?[\\s\\n\\r\\t]*([\\u4e00-\\u9fa5\\u00b7]{2,4})(?![\\u4e00-\\u9fa5])").matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!isBlacklisted(name, blacklist) && !name.equals("姓名")) return name;
        }
            
        // 策略3: 表格行特征匹配 (名字 男/女 年龄) -> 强特征，专门针对表格排版
        // 匹配如 "张明 男 25" 或 "张明 女 28"
        m = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5\\u00b7]{2,4})\\s+(?:男|女)\\s+\\d{1,2}").matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!isBlacklisted(name, blacklist)) return name;
        }
            
        // 策略4: 表格列对齐特征 ("姓名"在上一行，名字在下一行)
        // 匹配 "姓名\n张明" 或 "姓名\r\n张明" 格式
        m = java.util.regex.Pattern.compile("姓名[\\s\\n\\r\\t]+([\\u4e00-\\u9fa5\\u00b7]{2,4})[\\s\\n\\r\\t]*(?:男|女|\\d{1,2}|[\\u4e00-\\u9fa5]{2,4}大学)").matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!isBlacklisted(name, blacklist) && !name.equals("姓名")) return name;
        }
            
        // 策略5: 全文寻找可能的中文名（2-4字），并校验上下文
        m = java.util.regex.Pattern.compile("([^\\u4e00-\\u9fa5])([\\u4e00-\\u9fa5\\u00b7]{2,4})([^\\u4e00-\\u9fa5])").matcher(text);
        while (m.find()) {
            String name = m.group(2);
            if (!isBlacklisted(name, blacklist) && !name.equals("简历") && !name.equals("个人") && !name.equals("姓名")) {
                int start = m.start(2);
                if (start > 0 && text.charAt(start - 1) == '姓') continue;
                return name;
            }
        }
            
        return null;
    }
    
    private boolean isBlacklisted(String text, String[] blacklist) {
        for (String word : blacklist) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private Integer extractAge(String text) {
        // 格式: 30岁 / 30 岁
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})\\s*岁").matcher(text);
        if (m.find()) {
            int age = Integer.parseInt(m.group(1));
            if (age >= 18 && age <= 70) return age;
        }
        // 格式: 出生年月:1995-01 或 生日:1995年
        m = java.util.regex.Pattern.compile("(?:出生|生日|出生日期)[：:]\\s*(\\d{4})[年-]").matcher(text);
        if (m.find()) {
            int birthYear = Integer.parseInt(m.group(1));
            int currentYear = java.time.LocalDate.now().getYear();
            int age = currentYear - birthYear;
            if (age >= 18 && age <= 70) return age;
        }
        return null;
    }

    private String extractEducation(String text) {
        // 匹配 学历:本科 / 教育程度:硕士
        for (String kw : new String[]{"学历[：:]\\s*", "教育[：:]\\s*", "教育程度[：:]\\s*"}) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(kw).matcher(text);
            if (m.find()) {
                String after = text.substring(m.end());
                for (String level : new String[]{"博士", "硕士", "研究生", "本科", "大专", "专科", "高中", "初中"}) {
                    if (after.startsWith(level) || after.contains(level)) return level;
                }
            }
        }
        // 直接匹配
        for (String level : new String[]{"博士", "硕士", "研究生", "本科", "大专", "专科"}) {
            if (text.contains(level)) return level;
        }
        return null;
    }

    private String extractExperience(String text) {
        // 格式: 5年工作经验 / 5年 工作经验 / 工作经验5年
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*年\\s*工作\s*经验").matcher(text);
        if (m.find()) return m.group(1) + "年工作经验";
        // 格式: 工作\s*经验[：:]\\s*(\\d+)
        m = java.util.regex.Pattern.compile("工作\s*经验[：:]\\s*(\\d+)").matcher(text);
        if (m.find()) return m.group(1) + "年工作经验";
        // 格式: \\d+\\s*年(?:后端|前端|开发|Java|编程)
        m = java.util.regex.Pattern.compile("(\\d+)\\s*年(?:后端|前端|开发|Java|编程|技术|行业|从业|工作)").matcher(text);
        if (m.find()) return m.group(1) + "年工作经验";
        return null;
    }

    private String extractSkills(String text) {
        // 先匹配 技能[：:] 后面的内容
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("技能[：:]\\s*([^\\n]{2,300})").matcher(text);
        if (m.find()) {
            String skillsText = m.group(1).trim();
            // 按逗号、顿号、空格分割
            String[] tokens = skillsText.split("[,，、\\s]+");
            List<String> found = new java.util.ArrayList<>();
            for (String t : tokens) {
                String clean = t.trim();
                if (clean.length() > 1 && clean.length() < 30) {
                    found.add(clean);
                }
            }
            if (!found.isEmpty()) {
                return skillsToJson(found);
            }
        }
        
        // 如果没找到，用关键词匹配
        String[] keywords = {"Java","Python","C++","JavaScript","Vue","React","Spring","MySQL","Redis",
            "Docker","Linux","HTML","CSS","微服务","分布式","高并发",
            "教学","授课","备课","教研","教师资格证","SQL","Git","Office","Excel","Word",
            "Photoshop","Illustrator","Axure","Figma",
            "沟通","团队协作","项目管理","数据分析","市场营销"};
        List<String> found = new java.util.ArrayList<>();
        for (String kw : keywords) {
            if (text.toLowerCase().contains(kw.toLowerCase())) found.add(kw);
        }
        if (found.isEmpty()) return null;
        return skillsToJson(found);
    }
    
    private String skillsToJson(List<String> skills) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < skills.size(); i++) {
            sb.append("\"").append(skills.get(i)).append("\"");
            if (i < skills.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractWorkExperience(String text) {
        // 简单匹配工作经历标题后的内容
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:工作\\s*经历|工作\\s*经验|职业\\s*经历)[：:]?[\\s\\S]*?((?:\\d{4}[\\-年]\\d{1,2}[\\-月]?)\\s*(?:至|到|\\-)?\\s*(?:\\d{4}[\\-年]\\d{1,2}[\\-月]?|至今).*?(?:\\n|$)){1,5}").matcher(text);
        
        if (m.find()) {
            String workText = m.group(0);
            // 简单提取公司名称和职位
            java.util.regex.Matcher companyM = java.util.regex.Pattern.compile("((?:有限公司|集团|公司|科技|网络|软件|技术)[\\s\\S]{0,50})").matcher(workText);
            if (companyM.find()) {
                return companyM.group(1).trim();
            }
        }
        
        return null; // 复杂解析交给AI
    }

    private String extractSelfIntro(String text) {
        return null;
    }

    // 从 MultipartFile 直接提取文本（无需落盘，更快）
    private String extractTextFromMultipartFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null) return "";
            String ext = fileName.toLowerCase();
            
            if (ext.endsWith(".pdf")) {
                return documentParseService.extractTextFromMultipartFileBytes(file.getBytes(), fileName);
            } else if (ext.endsWith(".docx")) {
                return documentParseService.extractTextFromMultipartFileBytes(file.getBytes(), fileName);
            } else if (ext.endsWith(".doc")) {
                return documentParseService.extractTextFromMultipartFileBytes(file.getBytes(), fileName);
            }
            return "";
        } catch (Exception e) {
            log.warn("文本提取失败: {}", e.getMessage());
            return "";
        }
    }

    private Integer parseAgeFromObj(Object ageObj) {
        if (ageObj == null) return null;
        if (ageObj instanceof Integer) return (Integer) ageObj;
        if (ageObj instanceof Long) return ((Long) ageObj).intValue();
        if (ageObj instanceof String) {
            try {
                String ageStr = ((String) ageObj).replaceAll("[^0-9]", "");
                if (!ageStr.isEmpty()) {
                    int age = Integer.parseInt(ageStr);
                    if (age >= 18 && age <= 70) return age;
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String ensureJsonArray(String str) {
        if (str == null || str.isEmpty()) return "[]";
        str = str.trim();
        if (str.startsWith("[") && str.endsWith("]")) {
            // 已是JSON数组，检查是否需要拆分逗号拼接的元素
            // 简单判断：如果数组内只有一个元素且包含逗号，说明是拼接的
            String inner = str.substring(1, str.length() - 1).trim();
            if (inner.startsWith("\"") && inner.endsWith("\"") && !inner.substring(1, inner.length() - 1).contains("\",\"")) {
                // 单个元素的数组，如 ["分布式系统, 云计算, JVM调优"]
                String single = inner.substring(1, inner.length() - 1); // 去掉引号
                if (single.contains(",") || single.contains("，") || single.contains("、") || single.contains(";") || single.contains("；")) {
                    String[] parts = single.split("[,，、;；]+");
                    StringBuilder rebuild = new StringBuilder("[");
                    for (int i = 0; i < parts.length; i++) {
                        String p = parts[i].trim();
                        if (!p.isEmpty()) {
                            if (rebuild.length() > 1) rebuild.append(",");
                            rebuild.append("\"").append(p.replace("\"", "\\\"")).append("\"");
                        }
                    }
                    rebuild.append("]");
                    return rebuild.toString();
                }
            }
            return str;
        }
        // 逗号分隔的字符串转JSON数组
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

    @PostMapping("/{id}/ai-analyze")
    public ResponseEntity<?> analyzeResume(@PathVariable Long id) {
        try {
            Resume analyzedResume = resumeService.analyzeResumeWithAI(id);
            if (analyzedResume != null) {
                return ResponseEntity.ok(analyzedResume);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 从文档导入简历
     * 支持 PDF、DOC、DOCX 格式
     */
    @PostMapping("/import")
    public ResponseEntity<?> importResume(@RequestParam("file") MultipartFile file, Authentication auth) {
        try {
            String username = auth.getName();
            com.smartrecruitment.entity.User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            Resume resume = resumeService.importResumeFromDocument(user.getId(), file);
            return ResponseEntity.ok(resume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 预解析文档（不保存，仅返回解析结果）
     */
    @PostMapping("/parse-preview")
    public ResponseEntity<?> parseResumePreview(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> parsedData = documentParseService.parseResumeDocument(file);
            return ResponseEntity.ok(parsedData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 设置默认简历
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<?> setDefaultResume(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            Resume resume = resumeService.getById(id);
            if (resume == null || !resume.getUserId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            }
            
            // 将该用户的所有简历设为非默认
            resumeService.lambdaUpdate()
                    .eq(Resume::getUserId, user.getId())
                    .set(Resume::getIsDefault, 0)
                    .update();
            
            // 将指定简历设为默认
            resume.setIsDefault(1);
            resumeService.updateById(resume);
            
            operationLogService.log(
                    user.getId(),
                    user.getUsername(),
                    "设置",
                    "简历",
                    String.format("将 [%s] 设为默认简历", resume.getResumeName()),
                    IpUtils.getClientIp(),
                    1
            );
            
            return ResponseEntity.ok(Map.of("message", "设置成功", "resumeId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 复制简历
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<?> copyResume(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            Resume originalResume = resumeService.getById(id);
            if (originalResume == null || !originalResume.getUserId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "简历不存在"));
            }
            
            // 创建新简历
            Resume newResume = new Resume();
            newResume.setUserId(user.getId());
            newResume.setResumeName(originalResume.getResumeName() + " (副本)");
            newResume.setCategoryId(originalResume.getCategoryId());
            newResume.setName(originalResume.getName());
            newResume.setPhone(originalResume.getPhone());
            newResume.setEmail(originalResume.getEmail());
            newResume.setAge(originalResume.getAge());
            newResume.setExperience(originalResume.getExperience());
            newResume.setSkills(originalResume.getSkills());
            newResume.setEducation(originalResume.getEducation());
            newResume.setExpectedSalary(originalResume.getExpectedSalary());
            newResume.setWorkExperience(originalResume.getWorkExperience());
            newResume.setSelfIntroduction(originalResume.getSelfIntroduction());
            newResume.setAiAnalysis(originalResume.getAiAnalysis());
            newResume.setFileUrl(originalResume.getFileUrl());
            newResume.setIsDefault(0);
            
            resumeService.save(newResume);
            
            operationLogService.log(
                    user.getId(),
                    user.getUsername(),
                    "复制",
                    "简历",
                    String.format("复制了简历 [%s]", originalResume.getResumeName()),
                    IpUtils.getClientIp(),
                    1
            );
            
            return ResponseEntity.ok(newResume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
