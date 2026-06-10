package com.smartrecruitment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.config.BailianConfig;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.service.DocumentParseService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档解析服务实现 - 优化版
 * 使用AI深度解析，重点提取工作经历
 */
@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseServiceImpl.class);

    @Autowired
    private BailianConfig bailianConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> parseResumeDocument(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String text = extractTextFromMultipartFile(file);

        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("文档内容为空或无法解析");
        }

        // 清洗提取的文本
        text = cleanExtractedText(text);

        log.info("提取的简历文本长度: {}", text.length());
        log.debug("提取的简历文本前500字: {}", text.substring(0, Math.min(500, text.length())));

        Map<String, Object> result = new HashMap<>();
        result.put("rawText", text);
        result.put("fileName", fileName);

        // 优先使用AI解析（更准确）
        try {
            Map<String, Object> aiResult = parseWithAI(text);
            if (aiResult != null && !aiResult.isEmpty()) {
                result.putAll(aiResult);
                result.put("parseMethod", "ai");
                log.info("AI解析成功，提取到{}个字段", aiResult.size());
                return result;
            }
        } catch (Exception e) {
            log.warn("AI解析失败，使用正则提取: {}", e.getMessage());
        }

        // 降级到正则提取
        result.put("name", extractName(text));
        result.put("age", extractAge(text));
        result.put("skills", extractSkills(text));
        result.put("experience", extractExperience(text));
        result.put("education", extractEducation(text));
        result.put("expectedSalary", extractExpectedSalary(text));
        result.put("workExperience", extractWorkExperience(text));
        result.put("parseMethod", "regex");

        return result;
    }

    @Override
    public Resume parseResumeToEntity(MultipartFile file) {
        Map<String, Object> parsedData = parseResumeDocument(file);

        Resume resume = new Resume();
        resume.setName((String) parsedData.get("name"));
        resume.setPhone((String) parsedData.get("phone"));
        resume.setEmail((String) parsedData.get("email"));
        resume.setAge((Integer) parsedData.get("age"));

        // 确保skills字段是有效的JSON格式
        String skills = (String) parsedData.get("skills");
        resume.setSkills(ensureJsonArray(skills));

        resume.setExperience((String) parsedData.get("experience"));
        resume.setEducation((String) parsedData.get("education"));

        // 处理薪资
        Object salaryObj = parsedData.get("expectedSalary");
        if (salaryObj instanceof BigDecimal) {
            resume.setExpectedSalary((BigDecimal) salaryObj);
        } else if (salaryObj instanceof String) {
            try {
                String salaryStr = ((String) salaryObj).replaceAll("[^0-9.]", "");
                if (!salaryStr.isEmpty()) {
                    resume.setExpectedSalary(new BigDecimal(salaryStr));
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        // 确保workExperience字段是有效的JSON格式
        String workExp = (String) parsedData.get("workExperience");
        resume.setWorkExperience(ensureJsonArray(workExp));

        // 自我评价
        resume.setSelfIntroduction((String) parsedData.get("selfIntroduction"));

        // AI分析结果（与解析同次调用获取）
        resume.setAiAnalysis((String) parsedData.get("aiAnalysis"));

        return resume;
    }

    @Override
    public String extractTextFromFile(String filePath, String fileName) {
        try {
            Path path = Path.of(filePath);
            byte[] fileBytes = Files.readAllBytes(path);

            if (fileName.toLowerCase().endsWith(".pdf")) {
                return extractTextFromPdf(fileBytes);
            } else if (fileName.toLowerCase().endsWith(".docx")) {
                return extractTextFromDocx(fileBytes);
            } else if (fileName.toLowerCase().endsWith(".doc")) {
                return extractTextFromDoc(fileBytes);
            } else {
                throw new RuntimeException("不支持的文件格式");
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public String extractTextFromMultipartFileBytes(byte[] fileBytes, String fileName) {
        try {
            if (fileName.toLowerCase().endsWith(".pdf")) {
                return extractTextFromPdf(fileBytes);
            } else if (fileName.toLowerCase().endsWith(".docx")) {
                return extractTextFromDocx(fileBytes);
            } else if (fileName.toLowerCase().endsWith(".doc")) {
                return extractTextFromDoc(fileBytes);
            } else {
                throw new RuntimeException("不支持的文件格式");
            }
        } catch (Exception e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 使用AI智能解析简历 - 优化版，重点提取工作经历
     */
    private Map<String, Object> parseWithAI(String text) {
        String prompt = buildAIParsePrompt(text);
        String response = bailianConfig.call(prompt);

        log.info("========== AI解析开始 ==========");
        log.info("AI解析响应长度: {}", response != null ? response.length() : 0);
        log.debug("AI解析原始响应前1000字符: {}", response != null && response.length() > 1000 ? response.substring(0, 1000) : response);

        try {
            // 清理响应中的非JSON内容
            String jsonStr = response.trim();
            
            log.info("步骤1 - 原始响应长度: {}", jsonStr.length());
            
            // 优先查找```json代码块
            int codeBlockStart = jsonStr.indexOf("```json");
            if (codeBlockStart != -1) {
                int codeBlockEnd = jsonStr.indexOf("```", codeBlockStart + 7);
                if (codeBlockEnd != -1) {
                    jsonStr = jsonStr.substring(codeBlockStart + 7, codeBlockEnd).trim();
                    log.info("步骤2 - 从```json代码块提取，长度: {}", jsonStr.length());
                }
            } else {
                // 如果没有代码块，直接提取第一个{到最后一个}
                int jsonStart = jsonStr.indexOf("{");
                int jsonEnd = jsonStr.lastIndexOf("}");
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    jsonStr = jsonStr.substring(jsonStart, jsonEnd + 1);
                    log.info("步骤2 - 直接提取JSON，长度: {}", jsonStr.length());
                } else {
                    log.error("步骤2 - 【失败】未找到JSON结构！");
                    return null;
                }
            }
            
            log.debug("步骤3 - JSON内容前500字符: {}", jsonStr.length() > 500 ? jsonStr.substring(0, 500) : jsonStr);

            // 解析AI返回的JSON
            Map<String, Object> aiResult = objectMapper.readValue(jsonStr, Map.class);
            log.info("步骤4 - JSON解析成功，包含{}个字段", aiResult.size());
            log.info("步骤4 - 字段列表: {}", aiResult.keySet());
            
            // 验证关键字段
            if (!aiResult.containsKey("basic_info") && !aiResult.containsKey("work_experience_list")) {
                log.error("步骤4 - 【失败】AI返回的JSON缺少关键字段！");
                return null;
            }
            
            Map<String, Object> result = new HashMap<>();

            // 映射AI返回的字段
            if (aiResult.containsKey("basic_info")) {
                Map<String, Object> basicInfo = (Map<String, Object>) aiResult.get("basic_info");
                log.info("步骤5 - 解析基本信息: {}", basicInfo);
                result.put("name", basicInfo.get("name"));
                result.put("age", parseAge(basicInfo.get("age")));
                result.put("phone", basicInfo.get("phone"));
                result.put("email", basicInfo.get("email"));
            } else {
                log.warn("步骤5 - AI返回数据中缺少basic_info字段");
            }

            // 处理技能列表 - 转换为JSON数组格式
            if (aiResult.containsKey("skill_list")) {
                List<String> skills = (List<String>) aiResult.get("skill_list");
                log.info("步骤6 - 解析到{}个技能", skills != null ? skills.size() : 0);
                result.put("skills", convertToJsonArray(skills));
            } else {
                log.warn("步骤6 - AI返回数据中缺少skill_list字段");
            }

            // 处理教育经历
            if (aiResult.containsKey("education_list")) {
                List<Map<String, Object>> eduList = (List<Map<String, Object>>) aiResult.get("education_list");
                if (eduList != null && !eduList.isEmpty()) {
                    log.info("步骤7 - 解析到{}段教育经历", eduList.size());
                    Map<String, Object> edu = eduList.get(0);
                    StringBuilder eduStr = new StringBuilder();
                    eduStr.append(edu.getOrDefault("school_name", ""));
                    eduStr.append(" ").append(edu.getOrDefault("education", ""));
                    eduStr.append(" ").append(edu.getOrDefault("major", ""));
                    String eduResult = eduStr.toString().trim();
                    if (!eduResult.isEmpty()) {
                        result.put("education", eduResult);
                        log.info("步骤7 - 教育背景: {}", eduResult);
                    }
                } else {
                    log.warn("步骤7 - education_list为空");
                }
            } else {
                log.warn("步骤7 - AI返回数据中缺少education_list字段");
            }

            // 处理工作经历 - 重点优化，映射AI字段到前端表单字段
            if (aiResult.containsKey("work_experience_list")) {
                List<Map<String, Object>> workList = (List<Map<String, Object>>) aiResult.get("work_experience_list");
                if (workList != null && !workList.isEmpty()) {
                    log.info("步骤8 - AI解析到{}段工作经历", workList.size());
                    
                    // 映射AI返回的字段名到前端表单期望的字段名
                    List<Map<String, Object>> mappedList = new ArrayList<>();
                    int totalYears = 0;
                    
                    for (int i = 0; i < workList.size(); i++) {
                        Map<String, Object> work = workList.get(i);
                        Map<String, Object> mapped = new HashMap<>();
                        
                        log.debug("步骤8.{} - 原始工作数据: {}", i+1, work);
                        
                        // 提取公司名称（支持多种字段名）
                        String company = (String) work.getOrDefault("company", 
                            work.getOrDefault("company_name", 
                            work.getOrDefault("companyName", "")));
                        mapped.put("company", company);
                        
                        // 提取职位
                        String position = (String) work.getOrDefault("position", 
                            work.getOrDefault("title", ""));
                        mapped.put("position", position);
                        
                        // 处理时间格式
                        String startTime = (String) work.getOrDefault("start_time", 
                            work.getOrDefault("startDate", ""));
                        String endTime = (String) work.getOrDefault("end_time", 
                            work.getOrDefault("endDate", ""));
                        
                        // 标准化时间格式为 YYYY-MM
                        String normalizedStart = normalizeTimeFormat(startTime);
                        String normalizedEnd = normalizeTimeFormat(endTime);
                        mapped.put("startDate", normalizedStart);
                        mapped.put("endDate", normalizedEnd);
                        mapped.put("current", "至今".equals(endTime) || Boolean.TRUE.equals(work.get("current")));
                        
                        // 工作内容描述（支持多种字段名）
                        String description = (String) work.getOrDefault("description", 
                            work.getOrDefault("work_content", 
                            work.getOrDefault("workContent", "")));
                        mapped.put("description", description);
                        
                        log.info("步骤8.{} - 工作经历: 公司={}, 职位={}, 时间={}至{}, 在职={}", 
                            i+1, company, position, normalizedStart, normalizedEnd, mapped.get("current"));
                        
                        mappedList.add(mapped);

                        // 计算工作年限
                        if (startTime != null && !startTime.isEmpty()) {
                            try {
                                int startYear = Integer.parseInt(startTime.substring(0, 4));
                                int endYear = ("至今".equals(endTime) || endTime == null || endTime.isEmpty()) 
                                    ? Calendar.getInstance().get(Calendar.YEAR) 
                                    : Integer.parseInt(endTime.substring(0, 4));
                                totalYears += (endYear - startYear);
                            } catch (Exception e) {
                                log.warn("步骤8.{} - 工作年限计算失败: {}", i+1, e.getMessage());
                            }
                        }
                    }
                    
                    String workExpJson = objectMapper.writeValueAsString(mappedList);
                    result.put("workExperience", workExpJson);
                    log.info("步骤8 - 工作经历JSON长度: {}, 总工作年限: {}年", workExpJson.length(), totalYears);

                    if (totalYears > 0) {
                        result.put("experience", totalYears + "年工作经验");
                    }
                } else {
                    log.warn("步骤8 - work_experience_list为空数组");
                }
            } else {
                log.error("步骤8 - 【严重】AI返回数据中缺少work_experience_list字段！这会导致工作经历完全丢失");
                log.error("步骤8 - 请检查AI是否正确理解了Prompt要求");
            }

            // 处理自我评价
            if (aiResult.containsKey("self_introduction")) {
                String selfIntro = (String) aiResult.get("self_introduction");
                if (selfIntro != null && !selfIntro.isEmpty()) {
                    result.put("selfIntroduction", selfIntro);
                    log.info("步骤9 - 自我评价长度: {}", selfIntro.length());
                } else {
                    log.warn("步骤9 - self_introduction为空");
                }
            } else {
                log.warn("步骤9 - AI返回数据中缺少self_introduction字段");
            }

            // 处理AI分析结果（与解析同次调用，无需二次请求）
            if (aiResult.containsKey("ai_analysis")) {
                result.put("aiAnalysis", objectMapper.writeValueAsString(aiResult.get("ai_analysis")));
                log.info("步骤10 - 同步获取到AI分析结果");
            } else {
                log.warn("步骤10 - AI返回数据中缺少ai_analysis字段");
            }

            log.info("========== AI解析完成 ==========");
            log.info("最终解析结果汇总:");
            log.info("  - 姓名: {}", result.get("name"));
            log.info("  - 年龄: {}", result.get("age"));
            log.info("  - 电话: {}", result.get("phone"));
            log.info("  - 邮箱: {}", result.get("email"));
            log.info("  - 学历: {}", result.get("education"));
            log.info("  - 经验: {}", result.get("experience"));
            log.info("  - 技能数量: {}", result.containsKey("skills") ? ((String)result.get("skills")).length() : 0);
            log.info("  - 工作经历JSON长度: {}", result.containsKey("workExperience") ? ((String)result.get("workExperience")).length() : 0);
            log.info("  - 自我评价长度: {}", result.containsKey("selfIntroduction") ? ((String)result.get("selfIntroduction")).length() : 0);
            log.info("  - AI分析: {}", result.containsKey("aiAnalysis") ? "有" : "无");

            return result;
        } catch (Exception e) {
            log.error("========== AI解析失败 ==========");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            log.error("堆栈跟踪:", e);
            log.error("原始响应前1000字符: {}", response != null && response.length() > 1000 ? response.substring(0, 1000) : response);
            return null;
        }
    }
    
    /**
     * 标准化时间格式为 YYYY-MM
     */
    private String normalizeTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.isEmpty() || "至今".equals(timeStr)) {
            return "";
        }
        
        // 移除可能的空格
        timeStr = timeStr.trim();
        
        // 如果已经是 YYYY-MM 格式，直接返回
        if (timeStr.matches("\\d{4}-\\d{2}")) {
            return timeStr;
        }
        
        // 如果是 YYYY/MM/DD 或 YYYY-MM-DD 格式，提取年月
        if (timeStr.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) {
            return timeStr.substring(0, 7).replace('/', '-');
        }
        
        // 如果只是年份 YYYY
        if (timeStr.matches("\\d{4}")) {
            return timeStr + "-01";
        }
        
        // 其他格式，尝试提取年份
        if (timeStr.matches(".*\\d{4}.*")) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{4})");
            java.util.regex.Matcher m = p.matcher(timeStr);
            if (m.find()) {
                return m.group(1) + "-01";
            }
        }
        
        return "";
    }

    /**
     * 构建AI解析Prompt - 简化版，强调结构化输出
     */
    private String buildAIParsePrompt(String text) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一个简历解析专家。请仔细阅读以下简历全文，逐段逐行分析，提取所有信息。\n\n");
        sb.append("【必须提取的6大类字段】\n\n");
        sb.append("1. basic_info（基本信息）：\n");
        sb.append("   - name: 姓名\n");
        sb.append("   - age: 年龄（纯数字）\n");
        sb.append("   - phone: 手机号码\n");
        sb.append("   - email: 邮箱地址\n\n");
        sb.append("2. education_list（教育经历）：\n");
        sb.append("   遍历简历中所有教育/学历相关段落，提取每一段的：\n");
        sb.append("   - school_name: 学校名称\n");
        sb.append("   - education: 学历（博士/硕士/本科/大专等）\n");
        sb.append("   - major: 专业名称\n");
        sb.append("   注意：简历中可能有多段教育经历，全部列出\n\n");
        sb.append("3. work_experience_list（工作经历）：**最重要**\n");
        sb.append("   遍历简历中所有工作/实习/项目经历段落，逐条提取：\n");
        sb.append("   - company: 公司/单位名称\n");
        sb.append("   - position: 职位/岗位名称\n");
        sb.append("   - start_time: 入职时间（格式YYYY-MM）\n");
        sb.append("   - end_time: 离职时间（格式YYYY-MM，如在职则填\"至今\"）\n");
        sb.append("   - current: 是否在职（true/false）\n");
        sb.append("   - description: 工作内容/职责描述（尽量完整保留原文描述）\n");
        sb.append("   注意：简历中的每一段工作经历都必须提取，不能遗漏任何一段！\n\n");
        sb.append("4. skill_list（技能列表）：\n");
        sb.append("   提取简历中提到的所有技术栈、专业技能、工具等\n");
        sb.append("   - 每个技能单独一个元素，不要把多个技能拼在一个字符串里\n");
        sb.append("   - 例如：简历写\"Java, Spring Boot, MySQL\" → [\"Java\", \"Spring Boot\", \"MySQL\"]\n");
        sb.append("   - 中文技能也要逐个拆开：\"分布式系统, 云计算, JVM调优\" → [\"分布式系统\", \"云计算\", \"JVM调优\"]\n");
        sb.append("   - 复合术语保持完整：\"微服务架构设计\"不要拆成\"微服务\"和\"架构设计\"\n\n");
        sb.append("5. self_introduction（自我评价）：\n");
        sb.append("   提取简历中的自我评价/自我介绍/个人总结段落，完整保留原文\n\n");
        sb.append("6. ai_analysis（AI分析）：\n");
        sb.append("   对候选人做一个简短的综合分析评价（50-100字）\n\n");

        sb.append("【输出格式】严格按以下JSON格式输出，只输出JSON，不要任何其他文字：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"basic_info\": {\"name\": \"\", \"age\": null, \"phone\": \"\", \"email\": \"\"},\n");
        sb.append("  \"education_list\": [{\"school_name\": \"\", \"education\": \"\", \"major\": \"\"}],\n");
        sb.append("  \"work_experience_list\": [{\"company\": \"\", \"position\": \"\", \"start_time\": \"YYYY-MM\", \"end_time\": \"YYYY-MM\", \"current\": false, \"description\": \"\"}],\n");
        sb.append("  \"skill_list\": [],\n");
        sb.append("  \"self_introduction\": \"\",\n");
        sb.append("  \"ai_analysis\": {}\n");
        sb.append("}\n");
        sb.append("```\n\n");

        sb.append("【提取规则】\n");
        sb.append("1. 必须逐行扫描简历全文，确保不遗漏任何工作经历和教育经历\n");
        sb.append("2. 工作经历的description字段要尽量完整保留原始描述内容\n");
        sb.append("3. 自我评价要完整提取，不要截断\n");
        sb.append("4. 时间格式统一为YYYY-MM\n");
        sb.append("5. 某字段确实找不到时才填null，不要因为格式不标准就放弃提取\n\n");

        sb.append("【简历原文】\n");
        sb.append(text);

        return sb.toString();
    }

    /**
     * 将字符串列表转换为JSON数组格式
     */
    private String convertToJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将逗号分隔的字符串转换为JSON数组格式
     */
    private String convertToJsonArray(String skillsStr) {
        if (skillsStr == null || skillsStr.isEmpty()) {
            return "[]";
        }
        String[] skills = skillsStr.split("[,，、;；]+");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < skills.length; i++) {
            String skill = skills[i].trim();
            if (!skill.isEmpty()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("\"").append(skill.replace("\"", "\\\"")).append("\"");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 确保字符串是有效的JSON数组格式
     */
    private String ensureJsonArray(String str) {
        if (str == null || str.isEmpty()) {
            return "[]";
        }

        str = str.trim();

        // 如果已经是JSON数组格式，检查并拆分逗号拼接的元素
        if (str.startsWith("[") && str.endsWith("]")) {
            try {
                List<String> list = objectMapper.readValue(str, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                // 拆分包含逗号/顿号/分号的元素
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
                return convertToJsonArray(expanded);
            } catch (Exception e) {
                // 不是有效的JSON，需要转换
            }
        }

        // 转换为JSON数组格式
        return convertToJsonArray(str);
    }

    /**
     * 清洗提取的文本
     */
    private String cleanExtractedText(String text) {
        if (text == null) return "";

        // 移除页眉页脚
        text = text.replaceAll("(?i)page \\d+ of \\d+", "");
        text = text.replaceAll("(?i)第 \\d+ 页 / 共 \\d+ 页", "");

        // 移除多余空白字符，但保留换行
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");

        // 移除特殊字符
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return text.trim();
    }

    /**
     * 解析年龄
     */
    private Integer parseAge(Object ageObj) {
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
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        return null;
    }

    private String extractTextFromMultipartFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            if (fileName.toLowerCase().endsWith(".pdf")) {
                return extractTextFromPdf(file.getBytes());
            } else if (fileName.toLowerCase().endsWith(".docx")) {
                return extractTextFromDocx(file.getBytes());
            } else if (fileName.toLowerCase().endsWith(".doc")) {
                return extractTextFromDoc(file.getBytes());
            } else {
                throw new RuntimeException("不支持的文件格式");
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    private String extractTextFromPdf(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException("PDF 解析失败: " + e.getMessage());
        }
    }

    private String extractTextFromDocx(byte[] docxBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder text = new StringBuilder();

            // 提取所有段落和表格（按文档顺序）
            // 使用 bodyElements 获取文档中所有元素的正确顺序
            for (var bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof org.apache.xmlbeans.XmlCursor) {
                    continue;
                }
                String elementText = extractBodyElementText(bodyElement);
                if (elementText != null && !elementText.trim().isEmpty()) {
                    text.append(elementText).append("\n");
                }
            }

            // 如果 bodyElements 方式没有提取到内容，降级为分别提取段落和表格
            if (text.toString().trim().isEmpty()) {
                // 先提取段落
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String paraText = paragraph.getText();
                    if (paraText != null && !paraText.trim().isEmpty()) {
                        text.append(paraText).append("\n");
                    }
                }
                // 再提取表格
                for (var table : document.getTables()) {
                    for (var row : table.getRows()) {
                        for (var cell : row.getTableCells()) {
                            String cellText = cell.getText();
                            if (cellText != null && !cellText.trim().isEmpty()) {
                                text.append(cellText.trim()).append(" ");
                            }
                        }
                        text.append("\n");
                    }
                }
            }

            String result = text.toString().trim();
            log.info("DOCX文本提取完成，长度: {} 字符", result.length());
            return result;
        } catch (Exception e) {
            log.warn("DOCX高级提取失败，降级为段落提取: {}", e.getMessage());
            // 降级处理
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
                StringBuilder text = new StringBuilder();
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String paraText = paragraph.getText();
                    if (paraText != null && !paraText.trim().isEmpty()) {
                        text.append(paraText).append("\n");
                    }
                }
                return text.toString();
            } catch (IOException ex) {
                throw new RuntimeException("DOCX 解析失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 提取文档body元素的文本（段落或表格）
     */
    private String extractBodyElementText(Object element) {
        try {
            if (element instanceof XWPFParagraph) {
                return ((XWPFParagraph) element).getText();
            } else if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable) {
                org.apache.poi.xwpf.usermodel.XWPFTable table = (org.apache.poi.xwpf.usermodel.XWPFTable) element;
                StringBuilder sb = new StringBuilder();
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            sb.append(cellText.trim()).append(" ");
                        }
                    }
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                        sb.append("\n");
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.debug("提取body元素文本失败: {}", e.getMessage());
        }
        return null;
    }

    private String extractTextFromDoc(byte[] docBytes) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(docBytes))) {
            Range range = document.getRange();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < range.numParagraphs(); i++) {
                String paraText = range.getParagraph(i).text();
                if (paraText != null && !paraText.trim().isEmpty()) {
                    text.append(paraText).append("\n");
                }
            }
            return text.toString();
        } catch (IOException e) {
            throw new RuntimeException("DOC 解析失败: " + e.getMessage());
        }
    }

    private String extractName(String text) {
        String[] patterns = {
            "姓名[：:]\\s*([\\u4e00-\\u9fa5]{2,4})",
            "([\\u4e00-\\u9fa5]{2,4})\\s*的简历",
            "姓\\s*名[：:]\\s*([\\u4e00-\\u9fa5]{2,4})"
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.length() >= 2 && line.length() <= 10) {
                return line;
            }
        }
        return "未知";
    }

    private Integer extractAge(String text) {
        String[] patterns = {
            "年龄[：:]\\s*(\\d{1,2})",
            "(\\d{1,2})\\s*岁",
            "出生[年日期]*[：:]\\s*\\d{4}"
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                try {
                    String ageStr = matcher.group(1);
                    int age = Integer.parseInt(ageStr);
                    if (age >= 18 && age <= 70) {
                        return age;
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
        return null;
    }

    private String extractSkills(String text) {
        // 长的复合词在前，短的在后，避免"分布式"匹配掉"分布式系统"
        String[] skillKeywords = {
            // 复合技能（长词优先）
            "微服务架构设计", "服务注册与发现", "分布式事务", "分布式锁", "分布式系统",
            "Spring Cloud", "Spring Boot", "Spring MVC", "Spring Security",
            "JVM调优", "熔断降级", "配置中心", "分库分表", "云原生", "云计算",
            "服务治理", "链路追踪", "日志收集", "容器编排", "持续集成", "持续部署",
            "负载均衡", "高并发", "高可用", "系统设计", "架构设计", "数据结构",
            "消息队列", "缓存设计", "读写分离", "主从复制", "数据库优化",
            "代码审查", "单元测试", "集成测试", "压力测试", "性能优化",
            "敏捷开发", "项目管理", "数据分析", "数据挖掘", "机器学习", "深度学习",
            "计算机视觉", "自然语言处理", "推荐系统", "搜索引擎",
            // 编程语言
            "Java", "Python", "C++", "C#", "JavaScript", "TypeScript", "Go", "Rust", "PHP",
            "Kotlin", "Scala", "Swift", "Objective-C", "Ruby", "Lua", "Shell",
            // 后端框架
            "Spring", "MyBatis", "MyBatis-Plus", "Hibernate", "Django", "Flask", "Express",
            "Node.js", "NestJS", "Gin", "Fiber", "Laravel", "ThinkPHP",
            // 前端框架
            "Vue", "React", "Angular", "jQuery", "Bootstrap", "Element UI", "Element Plus",
            "Ant Design", "Tailwind CSS", "Next.js", "Nuxt.js",
            // 数据库
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Oracle", "SQL Server",
            "Elasticsearch", "ClickHouse", "TiDB", "Neo4j", "InfluxDB", "HBase",
            // 中间件
            "RabbitMQ", "Kafka", "RocketMQ", "ActiveMQ", "Zookeeper", "Nacos",
            "Consul", "Eureka", "Gateway", "Sentinel", "Seata", "Dubbo", "gRPC",
            "Nginx", "Apache", "Tomcat", "Netty",
            // 大数据
            "Hadoop", "Spark", "Flink", "Hive", "Presto", "Airflow", "DataX",
            // DevOps
            "Docker", "Kubernetes", "Jenkins", "Git", "Linux", "Ansible", "Terraform",
            "Prometheus", "Grafana", "ELK", "CI/CD", "DevOps",
            // AI/ML
            "PyTorch", "TensorFlow", "NLP", "BERT", "GPT", "LLM", "RAG",
            // 移动端
            "Android", "iOS", "Flutter", "React Native", "小程序", "uni-app",
            // 基础
            "HTML", "CSS", "SASS", "LESS", "Webpack", "Vite", "Babel",
            "REST", "GraphQL", "WebSocket", "TCP/IP", "HTTP", "HTTPS",
            "设计模式", "算法", "Scrum", "大数据", "人工智能", "微服务", "分布式",
            "集群", "缓存"
        };

        List<String> foundSkills = new ArrayList<>();
        String textLower = text.toLowerCase();

        for (String skill : skillKeywords) {
            if (textLower.contains(skill.toLowerCase())) {
                // 如果已包含更长的相关词，跳过短词（避免"分布式"和"分布式系统"同时出现）
                boolean dominated = false;
                for (String existing : foundSkills) {
                    if (existing.toLowerCase().contains(skill.toLowerCase()) && !existing.equalsIgnoreCase(skill)) {
                        dominated = true;
                        break;
                    }
                }
                if (!dominated) {
                    foundSkills.add(skill);
                }
            }
        }

        if (foundSkills.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < foundSkills.size(); i++) {
            sb.append("\"").append(foundSkills.get(i).replace("\"", "\\\"")).append("\"");
            if (i < foundSkills.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractExperience(String text) {
        String[] patterns = {
            "(\\d+)\\s*[年]\\s*工作经验",
            "工作经验[：:]\\s*(\\d+)\\s*年",
            "(\\d+)\\s*[年]\\s*开发经验",
            "工作年限[：:]\\s*(\\d+)\\s*年",
            "(\\d+)\\s*[年]\\s*以上.*经验"
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                return matcher.group(1) + "年工作经验";
            }
        }
        return null;
    }

    private String extractEducation(String text) {
        String[] eduLevels = {"博士", "硕士", "研究生", "本科", "大专", "专科"};
        for (String level : eduLevels) {
            if (text.contains(level)) {
                return level;
            }
        }
        return null;
    }

    private java.math.BigDecimal extractExpectedSalary(String text) {
        String[] patterns = {
            "期望薪资[：:]\\s*(\\d+)",
            "薪资期望[：:]\\s*(\\d+)",
            "(\\d+)\\s*[kK]",
            "(\\d+)\\s*元/月"
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                try {
                    String salaryStr = matcher.group(1);
                    java.math.BigDecimal salary = new java.math.BigDecimal(salaryStr);
                    if (text.toLowerCase().contains("k") && salary.compareTo(new java.math.BigDecimal(100)) < 0) {
                        salary = salary.multiply(new java.math.BigDecimal(1000));
                    }
                    return salary;
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
        return null;
    }

    private String extractWorkExperience(String text) {
        // 更全面的工作经历提取
        List<Map<String, String>> experiences = new ArrayList<>();

        // 匹配公司名和职位的模式
        String[] companyPatterns = {
            "([\\u4e00-\\u9fa5()（）]+(?:公司|集团|科技|技术|网络|信息|软件|互联网|有限|股份))",
            "([\\u4e00-\\u9fa5()（）]+(?:Co\\.?|Ltd\\.?|Inc\\.?))"
        };

        String[] positionPatterns = {
            "([\\u4e00-\\u9fa5]+(?:工程师|开发|架构|经理|主管|总监|设计师|测试|运维|产品|前端|后端|全栈))",
            "([\\u4e00-\\u9fa5]+(?:专员|助理|顾问|分析师|研究员))"
        };

        for (String companyPattern : companyPatterns) {
            Matcher companyMatcher = Pattern.compile(companyPattern).matcher(text);
            while (companyMatcher.find()) {
                Map<String, String> exp = new HashMap<>();
                exp.put("company", companyMatcher.group(1));
                experiences.add(exp);
            }
        }

        for (int i = 0; i < experiences.size(); i++) {
            for (String positionPattern : positionPatterns) {
                Matcher positionMatcher = Pattern.compile(positionPattern).matcher(text);
                if (positionMatcher.find()) {
                    experiences.get(i).put("position", positionMatcher.group(1));
                    break;
                }
            }
        }

        if (!experiences.isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < experiences.size(); i++) {
                Map<String, String> exp = experiences.get(i);
                sb.append("{\"company\":\"").append(exp.getOrDefault("company", "").replace("\"", "\\\""));
                sb.append("\",\"position\":\"").append(exp.getOrDefault("position", "").replace("\"", "\\\""));
                sb.append("\"}");
                if (i < experiences.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return null;
    }
}
