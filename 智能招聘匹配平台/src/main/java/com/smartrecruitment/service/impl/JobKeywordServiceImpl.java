package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.config.BailianConfig;
import com.smartrecruitment.entity.JobKeyword;
import com.smartrecruitment.entity.JobKeywordRelation;
import com.smartrecruitment.mapper.JobKeywordMapper;
import com.smartrecruitment.mapper.JobKeywordRelationMapper;
import com.smartrecruitment.service.JobKeywordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位关键词服务实现
 */
@Service
public class JobKeywordServiceImpl extends ServiceImpl<JobKeywordMapper, JobKeyword> implements JobKeywordService {

    private static final Logger log = LoggerFactory.getLogger(JobKeywordServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JobKeywordRelationMapper relationMapper;

    @Autowired
    private BailianConfig bailianConfig;

    // 关键词分类列表
    private static final List<String> CATEGORIES = Arrays.asList(
        "编程语言", "前端框架", "后端框架", "数据库", "开发工具", "概念", "行业", "软技能"
    );

    // 扩展的关键词库（按分类）
    private static final Map<String, List<String>> KEYWORD_LIBRARY = new HashMap<>();

    static {
        KEYWORD_LIBRARY.put("编程语言", Arrays.asList(
            "Java", "Python", "Go", "Golang", "C++", "C#", "JavaScript", "TypeScript",
            "PHP", "Ruby", "Scala", "Rust", "Swift", "Kotlin", "Dart", "Perl"
        ));
        KEYWORD_LIBRARY.put("前端框架", Arrays.asList(
            "Vue", "React", "Angular", "jQuery", "Bootstrap", "Element UI", "Ant Design",
            "Next.js", "Nuxt.js", "Svelte", "Tailwind CSS"
        ));
        KEYWORD_LIBRARY.put("后端框架", Arrays.asList(
            "Spring", "Spring Boot", "Spring Cloud", "Django", "Flask", "FastAPI",
            ".NET", "Express", "Koa", "Laravel", "Gin", "Echo", "NestJS"
        ));
        KEYWORD_LIBRARY.put("数据库", Arrays.asList(
            "MySQL", "PostgreSQL", "Oracle", "SQL Server", "MongoDB", "Redis",
            "Elasticsearch", "Cassandra", "HBase", "Neo4j", "MariaDB"
        ));
        KEYWORD_LIBRARY.put("开发工具", Arrays.asList(
            "Docker", "K8s", "Kubernetes", "Jenkins", "Git", "CI/CD", "DevOps",
            "Linux", "Shell", "Nginx", "Apache", "Tomcat", "Zookeeper", "AWS",
            "Azure", "GCP", "阿里云", "腾讯云", "Maven", "Gradle", "npm"
        ));
        KEYWORD_LIBRARY.put("概念", Arrays.asList(
            "微服务", "分布式", "高并发", "网络编程", "容器编排", "架构设计",
            "gRPC", "Protobuf", "RESTful", "GraphQL", "WebSocket",
            "区块链", "Solidity", "智能合约", "以太坊", "Hyperledger", "Fabric", "DeFi", "密码学",
            "AI", "人工智能", "机器学习", "深度学习", "NLP", "计算机视觉", "大模型", "LLM",
            "大数据", "Hadoop", "Spark", "Flink", "Hive", "数据仓库",
            "移动端", "iOS", "Android", "Flutter", "React Native", "小程序",
            "游戏开发", "Unity", "Unreal", "Cocos",
            "网络安全", "渗透测试", "防火墙", "信息安全"
        ));
        KEYWORD_LIBRARY.put("行业", Arrays.asList(
            "电商", "金融", "教育", "医疗", "游戏", "社交", "物流", "旅游",
            "房地产", "制造业", "能源", "农业", "汽车", "区块链", "AI"
        ));
        KEYWORD_LIBRARY.put("软技能", Arrays.asList(
            "团队协作", "沟通能力", "问题解决", "学习能力", "领导力",
            "项目管理", "敏捷开发", "Scrum", "时间管理", "创新思维"
        ));
        // 医疗行业
        KEYWORD_LIBRARY.put("医疗-临床", Arrays.asList(
            "临床", "内科", "外科", "妇产科", "儿科", "心血管", "骨科", "肿瘤",
            "神经科", "呼吸科", "消化科", "泌尿科", "皮肤科", "眼科", "耳鼻喉",
            "口腔", "急诊", "重症", "麻醉", "全科", "中医", "中西医结合"
        ));
        KEYWORD_LIBRARY.put("医疗-护理", Arrays.asList(
            "护理", "护士", "护师", "主管护师", "护士资格", "临床护理",
            "ICU护理", "手术室护理", "社区护理", "老年护理", "康复护理"
        ));
        KEYWORD_LIBRARY.put("医疗-药学", Arrays.asList(
            "药学", "药剂", "执业药师", "临床药学", "药物分析", "药理学",
            "中药学", "药品管理", "处方审核", "药物不良反应"
        ));
        KEYWORD_LIBRARY.put("医疗-医技", Arrays.asList(
            "医学影像", "影像诊断", "B超", "CT", "MRI", "X光", "心电图",
            "检验", "病理", "康复", "针灸", "推拿", "放射治疗", "核医学"
        ));
        KEYWORD_LIBRARY.put("医疗-资质", Arrays.asList(
            "执业医师", "执业药师", "规培", "住院医师", "主治医师", "副主任医师",
            "主任医师", "医学硕士", "医学博士", "SCI", "课题", "科研"
        ));
        // 教育行业
        KEYWORD_LIBRARY.put("教育-教学", Arrays.asList(
            "教学", "授课", "备课", "课程设计", "教案", "教研", "班主任",
            "学科", "辅导", "答疑", "批改", "考试命题", "教学质量"
        ));
        KEYWORD_LIBRARY.put("教育-资质", Arrays.asList(
            "教师资格证", "普通话", "英语专八", "英语六级", "心理咨询师",
            "特级教师", "高级教师", "骨干教师", "学科带头人"
        ));
        // 金融行业
        KEYWORD_LIBRARY.put("金融-专业", Arrays.asList(
            "财务", "会计", "审计", "税务", "CFA", "CPA", "ACCA",
            "风控", "合规", "信贷", "投行", "基金", "证券", "保险",
            "精算", "理财", "资管", "信托", "期货", "外汇"
        ));
        // 制造/工程
        KEYWORD_LIBRARY.put("制造-工程", Arrays.asList(
            "机械", "电气", "自动化", "PLC", "CAD", "CAM", "SolidWorks",
            "质量管理", "工艺", "生产管理", "供应链", "精益生产", "六西格玛",
            "焊接", "模具", "CNC", "数控", "装配", "热处理"
        ));
        // 销售/市场
        KEYWORD_LIBRARY.put("销售-市场", Arrays.asList(
            "销售", "营销", "市场推广", "品牌", "渠道", "客户开发",
            "商务", "BD", "地推", "电销", "新媒体运营", "SEO", "SEM",
            "活动策划", "市场调研", "竞品分析", "用户增长"
        ));
    }

    @Override
    @Transactional
    public List<JobKeyword> extractKeywordsFromText(String text, String source) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<JobKeyword> extractedKeywords = new ArrayList<>();
        String textLower = text.toLowerCase();

        // 遍历所有分类的关键词库
        for (Map.Entry<String, List<String>> entry : KEYWORD_LIBRARY.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (textLower.contains(keyword.toLowerCase())) {
                    // 检查是否已存在
                    JobKeyword existing = getOne(new LambdaQueryWrapper<JobKeyword>()
                        .eq(JobKeyword::getKeyword, keyword));

                    if (existing != null) {
                        // 如果已存在，更新使用次数
                        existing.setUsageCount(existing.getUsageCount() + 1);
                        updateById(existing);
                        extractedKeywords.add(existing);
                    } else {
                        // 如果不存在，创建新关键词
                        JobKeyword newKeyword = new JobKeyword();
                        newKeyword.setKeyword(keyword);
                        newKeyword.setCategory(category);
                        newKeyword.setSource(source);
                        newKeyword.setUsageCount(1);
                        newKeyword.setIsActive(1);
                        save(newKeyword);
                        extractedKeywords.add(newKeyword);
                    }
                }
            }
        }

        return extractedKeywords;
    }

    @Override
    @Transactional
    public int linkKeywordsToJob(Long jobId, String text, String source) {
        if (jobId == null || text == null || text.isEmpty()) {
            return 0;
        }

        // 删除旧的关联
        relationMapper.delete(new LambdaQueryWrapper<JobKeywordRelation>()
            .eq(JobKeywordRelation::getJobId, jobId));

        // 提取关键词
        List<JobKeyword> extractedKeywords = extractKeywordsFromText(text, source);

        // 创建关联
        int count = 0;
        for (JobKeyword keyword : extractedKeywords) {
            // 检查是否已存在关联，避免重复插入
            Long existCount = relationMapper.selectCount(new LambdaQueryWrapper<JobKeywordRelation>()
                .eq(JobKeywordRelation::getJobId, jobId)
                .eq(JobKeywordRelation::getKeywordId, keyword.getId()));
            
            if (existCount == 0) {
                JobKeywordRelation relation = new JobKeywordRelation();
                relation.setJobId(jobId);
                relation.setKeywordId(keyword.getId());
                relation.setWeight(5);
                relationMapper.insert(relation);
                count++;
            }
        }

        return count;
    }

    @Override
    public Map<String, List<JobKeyword>> getKeywordsByCategory() {
        List<JobKeyword> allKeywords = list(new LambdaQueryWrapper<JobKeyword>()
            .eq(JobKeyword::getIsActive, 1)
            .orderByAsc(JobKeyword::getCategory)
            .orderByDesc(JobKeyword::getUsageCount));

        return allKeywords.stream()
            .collect(Collectors.groupingBy(
                k -> k.getCategory() != null ? k.getCategory() : "其他",
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    @Override
    public List<JobKeyword> getTopKeywords(int limit) {
        return list(new LambdaQueryWrapper<JobKeyword>()
            .eq(JobKeyword::getIsActive, 1)
            .orderByDesc(JobKeyword::getUsageCount)
            .last("LIMIT " + limit));
    }

    @Override
    @Transactional
    public void syncResumeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) return;
        log.info("syncResumeSkills收到{}个技能: {}", skills.size(), skills);

        // 展开：有些元素可能是逗号拼接的整串，需要拆开
        List<String> expanded = new ArrayList<>();
        for (String skill : skills) {
            if (skill == null) continue;
            String trimmed = skill.trim();
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

        // 收集库中没有的新技能
        List<String> newSkills = new ArrayList<>();
        for (String skill : expanded) {
            String trimmed = skill.trim();
            if (trimmed.isEmpty()) continue;

            // 先查数据库是否已有
            JobKeyword existing = getOne(new LambdaQueryWrapper<JobKeyword>()
                .eq(JobKeyword::getKeyword, trimmed));
            if (existing != null) {
                // 已有则增加使用次数
                existing.setUsageCount(existing.getUsageCount() + 1);
                updateById(existing);
            } else {
                newSkills.add(trimmed);
            }
        }

        if (newSkills.isEmpty()) return;

        // 用AI批量分类新技能
        Map<String, String> categorized = categorizeSkillsWithAI(newSkills);

        for (String skill : newSkills) {
            String category = categorized.getOrDefault(skill, "其他");

            // 再次检查避免重复（并发场景）
            JobKeyword dup = getOne(new LambdaQueryWrapper<JobKeyword>()
                .eq(JobKeyword::getKeyword, skill));
            if (dup != null) {
                dup.setUsageCount(dup.getUsageCount() + 1);
                updateById(dup);
                continue;
            }

            JobKeyword newKw = new JobKeyword();
            newKw.setKeyword(skill);
            newKw.setCategory(category);
            newKw.setSource("AI");
            newKw.setUsageCount(1);
            newKw.setIsActive(1);
            newKw.setDescription("求职者简历技能，AI自动识别分类");
            save(newKw);
            log.info("AI自动新增关键词: {} -> 分类: {}", skill, category);
        }
    }

    /**
     * 调用AI批量分类技能关键词
     */
    private Map<String, String> categorizeSkillsWithAI(List<String> skills) {
        Map<String, String> result = new HashMap<>();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("【任务】将以下技能关键词归类到最合适的分类中。\n\n");
            sb.append("【可选分类】\n");
            for (String cat : CATEGORIES) {
                sb.append("- ").append(cat).append("\n");
            }
            sb.append("- 其他（不属于以上任何分类时使用）\n\n");
            sb.append("【待分类技能】\n");
            for (int i = 0; i < skills.size(); i++) {
                sb.append(i + 1).append(". ").append(skills.get(i)).append("\n");
            }
            sb.append("\n【输出格式要求】只输出纯JSON对象，不要用markdown代码块包裹，不要添加任何解释文字：\n");
            sb.append("{\"分类结果\": {\"技能名\": \"分类名\", ...}}\n");

            String response = bailianConfig.call(sb.toString());
            log.info("AI分类返回原始响应: {}", response);

            // 清理AI返回的markdown代码块和多余文字
            String jsonStr = response.trim();
            int codeBlockStart = jsonStr.indexOf("```json");
            if (codeBlockStart == -1) codeBlockStart = jsonStr.indexOf("```");
            if (codeBlockStart != -1) {
                int codeBlockEnd = jsonStr.indexOf("```", codeBlockStart + 3);
                if (codeBlockEnd != -1) {
                    jsonStr = jsonStr.substring(
                        jsonStr.indexOf("\n", codeBlockStart) + 1, codeBlockEnd).trim();
                }
            } else {
                // 尝试提取JSON对象
                int jsonStart = jsonStr.indexOf("{");
                int jsonEnd = jsonStr.lastIndexOf("}");
                if (jsonStart != -1 && jsonEnd > jsonStart) {
                    jsonStr = jsonStr.substring(jsonStart, jsonEnd + 1);
                }
            }

            Map<String, Object> parsed = objectMapper.readValue(jsonStr, new TypeReference<>() {});
            Object raw = parsed.get("分类结果");
            if (raw instanceof Map) {
                Map<String, String> map = (Map<String, String>) raw;
                for (String skill : skills) {
                    String cat = map.get(skill);
                    if (cat != null && CATEGORIES.contains(cat)) {
                        result.put(skill, cat);
                    } else {
                        result.put(skill, "其他");
                    }
                }
            }
        } catch (Exception e) {
            log.error("AI分类技能失败, 将全部归入其他: {}", e.getMessage());
            for (String skill : skills) {
                result.put(skill, "其他");
            }
        }
        log.info("AI分类结果: {}", result);
        return result;
    }
}
