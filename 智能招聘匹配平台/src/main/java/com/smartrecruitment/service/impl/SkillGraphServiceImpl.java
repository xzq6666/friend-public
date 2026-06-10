package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.entity.SkillGraph;
import com.smartrecruitment.entity.SkillRelation;
import com.smartrecruitment.mapper.SkillGraphMapper;
import com.smartrecruitment.mapper.SkillRelationMapper;
import com.smartrecruitment.service.JobService;
import com.smartrecruitment.service.ResumeService;
import com.smartrecruitment.service.SkillGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 技能图谱服务实现
 */
@Service
public class SkillGraphServiceImpl extends ServiceImpl<SkillGraphMapper, SkillGraph> implements SkillGraphService {

    @Autowired
    private SkillRelationMapper relationMapper;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private com.smartrecruitment.service.JobCategoryService jobCategoryService;

    @Autowired
    private com.smartrecruitment.mapper.JobCategoryMapper jobCategoryMapper;

    // 行业关键词映射 - 优化后：区分真实技能与业务概念
    private static final Map<String, String[]> INDUSTRY_KEYWORDS = new LinkedHashMap<>();
    static {
        // IT/互联网 - 具体技术栈
        INDUSTRY_KEYWORDS.put("IT/互联网", new String[]{
            // 编程语言
            "java", "python", "javascript", "typescript", "go", "c++", "rust", "php",
            "swift", "kotlin", "scala", "ruby", "c#", "dart",
            // 前端技术
            "vue.js", "vue", "react", "angular", "jquery", "webpack", "vite",
            "html5", "css3", "sass", "less", "node.js", "nodejs",
            // 后端框架
            "spring boot", "spring cloud", "mybatis", "django", "flask", "fastapi",
            "express", "nestjs", "gin", "beego", "hibernate",
            // 数据库
            "mysql", "redis", "mongodb", "postgresql", "oracle", "elasticsearch",
            "mariadb", "sqlite", "cassandra", "neo4j",
            // 开发工具
            "docker", "kubernetes", "git", "jenkins", "maven", "gradle",
            "linux", "nginx", "apache", "tomcat", "rabbitmq", "kafka",
            // 云原生
            "aws", "阿里云", "腾讯云", "微服务", "devops", "ci/cd",
            // AI/大数据
            "tensorflow", "pytorch", "hadoop", "spark", "flink",
            "机器学习", "深度学习", "自然语言处理", "计算机视觉"
        });
        
        // 金融/财务 - 专业技能
        INDUSTRY_KEYWORDS.put("金融/财务", new String[]{
            "财务分析", "风险管理", "投资分析", "会计核算", "审计", "税务筹划",
            "金融建模", "信贷分析", "合规管理", "量化分析", "资产评估",
            "成本控制", "预算管理", "资金管理", "内控管理",
            "cpa", "cfa", "frm", "acca",
            "sap", "erp", "用友", "金蝶",
            "excel高级", "vba", "power bi", "tableau"
        });
        
        // 医疗/健康 - 专业技能
        INDUSTRY_KEYWORDS.put("医疗/健康", new String[]{
            "临床诊断", "药理学", "护理技术", "医学影像", "检验技术",
            "急救技能", "健康管理", "康复治疗", "手术操作",
            "his系统", "emr", "lis", "pac",
            "医疗器械", "gmp", "gsp"
        });
        
        // 教育/培训 - 专业技能
        INDUSTRY_KEYWORDS.put("教育/培训", new String[]{
            "教学设计", "课堂管理", "课程开发", "教育技术",
            "评估考核", "心理咨询", "在线教育", "教研能力",
            "课件制作", "翻转课堂", "混合式教学"
        });
        
        // 市场营销 - 专业技能
        INDUSTRY_KEYWORDS.put("市场营销", new String[]{
            "品牌策划", "数字营销", "内容营销", "市场调研", "活动策划",
            "广告投放", "seo", "sem", "新媒体运营", "社群运营",
            "文案写作", "视频剪辑", "pr", "ae", "ps", "ai",
            "google analytics", "百度统计", "crm"
        });
        
        // 设计/创意 - 专业技能
        INDUSTRY_KEYWORDS.put("设计/创意", new String[]{
            "ui设计", "ux设计", "交互设计", "品牌设计", "插画设计",
            "figma", "sketch", "photoshop", "illustrator", "after effects",
            "3d建模", "blender", "maya", "cinema 4d",
            "用户研究", "可用性测试", "设计系统"
        });
        
        // 制造业 - 专业技能
        INDUSTRY_KEYWORDS.put("制造业", new String[]{
            "质量管理", "生产管理", "供应链管理", "精益生产",
            "工艺工程", "设备维护", "cad", "cam", "solidworks",
            "auto cad", "pro e", "ug", "catia",
            "六西格玛", "iso9001", "iso14001", "5s管理",
            "erp系统", "mes", "plc", "scada"
        });
        
        // 销售/商务 - 使用完整技能词，避免部分匹配
        INDUSTRY_KEYWORDS.put("销售/商务", new String[]{
            "客户开发", "商务谈判", "客户管理", "销售策略",
            "方案演示", "合同管理", "渠道管理", "大客户销售",
            "招投标", "销售预测", "pipeline管理",
            "salesforce", "纷享销客", "销售易",
            // 零售/门店类
            "门店管理", "库存控制", "陈列规划", "客户服务",
            "门店运营", "收银管理", "理货", "盘点",
            "零售管理", "店员培训", "销售管理", "导购"
        });
        
        // 人力资源 - 专业技能
        INDUSTRY_KEYWORDS.put("人力资源", new String[]{
            "招聘管理", "培训发展", "薪酬福利", "绩效管理",
            "劳动关系", "组织发展", "面试技巧", "人才盘点",
            "胜任力模型", "薪酬调研", "员工关系",
            "北森", "moka", "sap hr"
        });
        
        // 法律 - 专业技能
        INDUSTRY_KEYWORDS.put("法律", new String[]{
            "法律研究", "合同审查", "诉讼仲裁", "合规管理",
            "知识产权", "公司法务", "法律文书", "尽职调查",
            "股权架构", "并购重组", "劳动法律"
        });
    }

    @Override
    public List<SkillGraph> getAllSkills() {
        return list();
    }

    @Override
    public List<SkillRelation> getAllRelations() {
        return relationMapper.selectList(null);
    }

    @Override
    public List<SkillGraph> getSkillsByCategory(String category) {
        return lambdaQuery().eq(SkillGraph::getCategory, category).list();
    }

    @Override
    public Map<String, Object> buildPersonalGraph(String skills) {
        List<SkillGraph> allSkills = getAllSkills();
        List<SkillRelation> allRelations = getAllRelations();

        // 解析个人技能列表
        Set<String> personalSkills = new HashSet<>();
        if (skills != null && !skills.isEmpty()) {
            String[] skillArray = skills.replace("[", "").replace("]", "")
                    .replace("\"", "").replace("'", "")
                    .replace("{", "").replace("}", "")
                    .split("[,，、;；\\s]+");
            for (String skill : skillArray) {
                skill = skill.trim();
                if (!skill.isEmpty()) {
                    personalSkills.add(normalizeSkillName(skill));
                }
            }
        }

        // 构建节点
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        // 动态收集所有分类（从技能库中获取）
        List<String> categories = allSkills.stream()
                .map(SkillGraph::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!categories.contains("其他")) categories.add("其他");

        // 添加个人技能中心节点
        Map<String, Object> centerNode = new HashMap<>();
        centerNode.put("name", "我的技能");
        centerNode.put("symbolSize", 60);
        centerNode.put("category", 0);
        centerNode.put("itemStyle", Map.of("color", "#5470c6"));
        nodes.add(centerNode);

        // 匹配技能并添加节点
        Map<String, Integer> skillIndexMap = new HashMap<>();
        skillIndexMap.put("我的技能", 0);
        int index = 1;

        for (SkillGraph skill : allSkills) {
            String normalized = normalizeSkillName(skill.getName());
            boolean isMatched = personalSkills.contains(normalized);

            if (isMatched) {
                Map<String, Object> node = new HashMap<>();
                node.put("name", skill.getName());
                node.put("symbolSize", 30 + (skill.getLevel() != null ? skill.getLevel() * 5 : 10));
                node.put("category", getCategoryIndex(skill.getCategory(), categories));

                if (skill.getLevel() != null && skill.getLevel() >= 4) {
                    node.put("itemStyle", Map.of("color", "#91cc75"));
                }

                nodes.add(node);
                skillIndexMap.put(skill.getName(), index);

                Map<String, Object> link = new HashMap<>();
                link.put("source", "我的技能");
                link.put("target", skill.getName());
                links.add(link);

                index++;
            }
        }

        // 添加技能之间的关系
        for (SkillRelation relation : allRelations) {
            SkillGraph sourceSkill = getById(relation.getSourceId());
            SkillGraph targetSkill = getById(relation.getTargetId());

            if (sourceSkill != null && targetSkill != null
                    && skillIndexMap.containsKey(sourceSkill.getName())
                    && skillIndexMap.containsKey(targetSkill.getName())) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", sourceSkill.getName());
                link.put("target", targetSkill.getName());
                link.put("value", relation.getWeight());
                links.add(link);
            }
        }

        // 构建分类
        List<Map<String, Object>> categoryList = categories.stream()
                .map(name -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("name", name);
                    return cat;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("categories", categoryList);
        return result;
    }

    /**
     * 标准化技能名称，用于精确匹配
     * vue.js -> vue, Vue.js -> vue, JavaScript -> javascript, etc.
     */
    private String normalizeSkillName(String name) {
        if (name == null) return "";
        String lower = name.toLowerCase().trim();
        // 去掉常见后缀
        lower = lower.replace(".js", "").replace(".net", "dotnet");
        // 去掉空格
        lower = lower.replace(" ", "");
        return lower;
    }

    @Override
    public Map<String, Object> buildJobGraph(String requirements) {
        List<SkillGraph> allSkills = getAllSkills();
        List<SkillRelation> allRelations = getAllRelations();

        // 解析职位要求中的技能
        Set<String> requiredSkills = new HashSet<>();
        if (requirements != null && !requirements.isEmpty()) {
            String reqLower = requirements.toLowerCase();
            for (SkillGraph skill : allSkills) {
                if (reqLower.contains(skill.getName().toLowerCase())) {
                    requiredSkills.add(skill.getName());
                }
            }
        }

        // 构建节点
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        List<String> categories = allSkills.stream()
                .map(SkillGraph::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!categories.contains("其他")) categories.add("其他");

        // 添加职位中心节点
        Map<String, Object> centerNode = new HashMap<>();
        centerNode.put("name", "职位要求");
        centerNode.put("symbolSize", 60);
        centerNode.put("category", 0);
        centerNode.put("itemStyle", Map.of("color", "#ee6666"));
        nodes.add(centerNode);

        Map<String, Integer> skillIndexMap = new HashMap<>();
        skillIndexMap.put("职位要求", 0);
        int index = 1;

        for (String skillName : requiredSkills) {
            SkillGraph skill = allSkills.stream()
                    .filter(s -> s.getName().equals(skillName))
                    .findFirst()
                    .orElse(null);

            if (skill != null) {
                Map<String, Object> node = new HashMap<>();
                node.put("name", skill.getName());
                node.put("symbolSize", 30 + (skill.getLevel() != null ? skill.getLevel() * 5 : 10));
                node.put("category", getCategoryIndex(skill.getCategory(), categories));
                nodes.add(node);
                skillIndexMap.put(skill.getName(), index);

                Map<String, Object> link = new HashMap<>();
                link.put("source", "职位要求");
                link.put("target", skill.getName());
                links.add(link);

                index++;
            }
        }

        // 添加技能之间的关系
        for (SkillRelation relation : allRelations) {
            SkillGraph sourceSkill = getById(relation.getSourceId());
            SkillGraph targetSkill = getById(relation.getTargetId());

            if (sourceSkill != null && targetSkill != null
                    && skillIndexMap.containsKey(sourceSkill.getName())
                    && skillIndexMap.containsKey(targetSkill.getName())) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", sourceSkill.getName());
                link.put("target", targetSkill.getName());
                link.put("value", relation.getWeight());
                links.add(link);
            }
        }

        List<Map<String, Object>> categoryList = categories.stream()
                .map(name -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("name", name);
                    return cat;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("categories", categoryList);
        return result;
    }

    @Override
    public List<String> getAllIndustries() {
        return list().stream()
                .map(SkillGraph::getIndustry)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillGraph> getSkillsByIndustry(String industry) {
        return lambdaQuery().eq(SkillGraph::getIndustry, industry).list();
    }

    @Override
    public Map<String, Object> buildPersonalGraphByIndustry(String skills, String industry) {
        List<SkillGraph> allSkills = getSkillsByIndustry(industry);
        List<SkillRelation> allRelations = getAllRelations();
        return buildGraphFromSkills(skills, allSkills, allRelations, "我的技能", "#5470c6");
    }

    @Override
    public Map<String, Object> buildJobGraphByIndustry(String requirements, String industry) {
        List<SkillGraph> allSkills = getSkillsByIndustry(industry);
        List<SkillRelation> allRelations = getAllRelations();
        return buildGraphFromRequirements(requirements, allSkills, allRelations);
    }

    private Map<String, Object> buildGraphFromSkills(String skills, List<SkillGraph> allSkills,
                                                      List<SkillRelation> allRelations,
                                                      String centerName, String centerColor) {
        Set<String> personalSkills = new HashSet<>();
        if (skills != null && !skills.isEmpty()) {
            for (String s : skills.replace("[", "").replace("]", "").replace("\"", "")
                    .split("[,，、;；\\s]+")) {
                s = s.trim();
                if (!s.isEmpty()) personalSkills.add(normalizeSkillName(s));
            }
        }

        List<String> categories = allSkills.stream()
                .map(SkillGraph::getCategory).distinct().collect(Collectors.toList());

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Map<String, Integer> skillIndexMap = new HashMap<>();

        Map<String, Object> center = new HashMap<>();
        center.put("name", centerName);
        center.put("symbolSize", 60);
        center.put("category", 0);
        center.put("itemStyle", Map.of("color", centerColor));
        nodes.add(center);
        skillIndexMap.put(centerName, 0);
        int idx = 1;

        for (SkillGraph skill : allSkills) {
            String normalized = normalizeSkillName(skill.getName());
            boolean matched = personalSkills.contains(normalized);
            if (matched) {
                Map<String, Object> node = new HashMap<>();
                node.put("name", skill.getName());
                node.put("symbolSize", 30 + (skill.getLevel() != null ? skill.getLevel() * 5 : 10));
                node.put("category", getCategoryIndex(skill.getCategory(), categories));
                nodes.add(node);
                skillIndexMap.put(skill.getName(), idx);

                Map<String, Object> link = new HashMap<>();
                link.put("source", centerName);
                link.put("target", skill.getName());
                links.add(link);
                idx++;
            }
        }

        for (SkillRelation rel : allRelations) {
            SkillGraph src = getById(rel.getSourceId());
            SkillGraph tgt = rel.getTargetId() != null ? getById(rel.getTargetId()) : null;
            if (src != null && tgt != null
                    && skillIndexMap.containsKey(src.getName())
                    && skillIndexMap.containsKey(tgt.getName())) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", src.getName());
                link.put("target", tgt.getName());
                link.put("value", rel.getWeight());
                links.add(link);
            }
        }

        List<Map<String, Object>> catList = categories.stream()
                .map(n -> { Map<String, Object> c = new HashMap<>(); c.put("name", n); return c; })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("categories", catList);
        return result;
    }

    private Map<String, Object> buildGraphFromRequirements(String requirements,
                                                            List<SkillGraph> allSkills,
                                                            List<SkillRelation> allRelations) {
        Set<String> requiredSkills = new HashSet<>();
        if (requirements != null && !requirements.isEmpty()) {
            String reqLower = requirements.toLowerCase();
            for (SkillGraph skill : allSkills) {
                if (reqLower.contains(skill.getName().toLowerCase())) {
                    requiredSkills.add(skill.getName());
                }
            }
        }

        List<String> categories = allSkills.stream()
                .map(SkillGraph::getCategory).distinct().collect(Collectors.toList());

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Map<String, Integer> skillIndexMap = new HashMap<>();

        Map<String, Object> center = new HashMap<>();
        center.put("name", "职位要求");
        center.put("symbolSize", 60);
        center.put("category", 0);
        center.put("itemStyle", Map.of("color", "#ee6666"));
        nodes.add(center);
        skillIndexMap.put("职位要求", 0);
        int idx = 1;

        for (String skillName : requiredSkills) {
            SkillGraph skill = allSkills.stream()
                    .filter(s -> s.getName().equals(skillName)).findFirst().orElse(null);
            if (skill != null) {
                Map<String, Object> node = new HashMap<>();
                node.put("name", skill.getName());
                node.put("symbolSize", 30 + (skill.getLevel() != null ? skill.getLevel() * 5 : 10));
                node.put("category", getCategoryIndex(skill.getCategory(), categories));
                nodes.add(node);
                skillIndexMap.put(skill.getName(), idx);

                Map<String, Object> link = new HashMap<>();
                link.put("source", "职位要求");
                link.put("target", skill.getName());
                links.add(link);
                idx++;
            }
        }

        for (SkillRelation rel : allRelations) {
            SkillGraph src = getById(rel.getSourceId());
            SkillGraph tgt = rel.getTargetId() != null ? getById(rel.getTargetId()) : null;
            if (src != null && tgt != null
                    && skillIndexMap.containsKey(src.getName())
                    && skillIndexMap.containsKey(tgt.getName())) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", src.getName());
                link.put("target", tgt.getName());
                link.put("value", rel.getWeight());
                links.add(link);
            }
        }

        List<Map<String, Object>> catList = categories.stream()
                .map(n -> { Map<String, Object> c = new HashMap<>(); c.put("name", n); return c; })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("categories", catList);
        return result;
    }

    @Override
    public void initSkillGraphData() {
        // 检查是否已初始化（如果已有带行业数据的记录则跳过）
        long total = count();
        if (total > 0) {
            long withIndustry = lambdaQuery().isNotNull(SkillGraph::getIndustry).count();
            if (withIndustry > 0) return;
            // 旧数据没有industry字段，清除后重新初始化
            remove(new LambdaQueryWrapper<>());
            relationMapper.delete(new LambdaQueryWrapper<>());
        }

        // {name, category, level, description, industry}
        String[][] industryData = {
            // ===== IT/互联网 =====
            {"Java", "编程语言", "5", "企业级应用开发首选语言", "IT/互联网"},
            {"Python", "编程语言", "5", "数据科学和AI领域主流语言", "IT/互联网"},
            {"JavaScript", "编程语言", "5", "Web前端开发必备语言", "IT/互联网"},
            {"TypeScript", "编程语言", "4", "JavaScript的超集，类型安全", "IT/互联网"},
            {"Go", "编程语言", "4", "云原生和微服务开发", "IT/互联网"},
            {"C++", "编程语言", "4", "系统级编程和游戏开发", "IT/互联网"},
            {"Rust", "编程语言", "3", "系统级安全编程", "IT/互联网"},
            {"PHP", "编程语言", "3", "Web后端开发", "IT/互联网"},
            {"Vue.js", "前端框架", "4", "渐进式JavaScript框架", "IT/互联网"},
            {"React", "前端框架", "5", "Facebook开发的UI库", "IT/互联网"},
            {"Angular", "前端框架", "4", "Google开发的前端框架", "IT/互联网"},
            {"Spring Boot", "后端框架", "5", "Java微服务框架", "IT/互联网"},
            {"Spring Cloud", "后端框架", "4", "微服务架构解决方案", "IT/互联网"},
            {"MyBatis", "后端框架", "4", "Java持久层框架", "IT/互联网"},
            {"Django", "后端框架", "4", "Python Web框架", "IT/互联网"},
            {"MySQL", "数据库", "5", "最流行的关系型数据库", "IT/互联网"},
            {"Redis", "数据库", "4", "内存缓存数据库", "IT/互联网"},
            {"MongoDB", "数据库", "4", "NoSQL文档数据库", "IT/互联网"},
            {"Docker", "开发工具", "4", "容器化技术", "IT/互联网"},
            {"Kubernetes", "开发工具", "4", "容器编排平台", "IT/互联网"},
            {"Git", "开发工具", "5", "版本控制工具", "IT/互联网"},
            {"Linux", "开发工具", "4", "服务器操作系统", "IT/互联网"},
            {"Nginx", "开发工具", "4", "Web服务器", "IT/互联网"},
            {"项目管理", "软技能", "3", "项目规划和执行能力", "IT/互联网"},
            {"团队协作", "软技能", "4", "团队合作和沟通能力", "IT/互联网"},

            // ===== 金融/财务 =====
            {"财务分析", "核心技能", "5", "财务报表分析与解读能力", "金融/财务"},
            {"风险管理", "核心技能", "5", "识别和控制金融风险", "金融/财务"},
            {"投资分析", "核心技能", "4", "证券、基金等投资产品分析", "金融/财务"},
            {"会计核算", "核心技能", "5", "会计准则与账务处理", "金融/财务"},
            {"审计", "核心技能", "4", "内部审计与合规检查", "金融/财务"},
            {"税务筹划", "核心技能", "4", "合理税务规划与申报", "金融/财务"},
            {"Excel高级", "工具技能", "5", "财务建模与数据分析", "金融/财务"},
            {"SAP/ERP", "工具技能", "4", "企业资源计划系统", "金融/财务"},
            {"Python量化", "工具技能", "3", "量化分析与算法交易", "金融/财务"},
            {"金融建模", "核心技能", "4", "DCF、LDM等估值模型", "金融/财务"},
            {"信贷分析", "核心技能", "4", "贷款风险评估", "金融/财务"},
            {"合规管理", "核心技能", "4", "金融监管合规", "金融/财务"},
            {"沟通能力", "软技能", "4", "与客户和团队有效沟通", "金融/财务"},
            {"数据分析", "软技能", "4", "数据驱动决策能力", "金融/财务"},

            // ===== 医疗/健康 =====
            {"临床诊断", "核心技能", "5", "疾病诊断与治疗方案", "医疗/健康"},
            {"药理学", "核心技能", "4", "药物作用机制与用药指导", "医疗/健康"},
            {"护理技术", "核心技能", "5", "基础护理与专科护理", "医疗/健康"},
            {"医学影像", "核心技能", "4", "X光、CT、MRI等影像诊断", "医疗/健康"},
            {"检验技术", "核心技能", "4", "临床检验与实验室分析", "医疗/健康"},
            {"病历管理", "管理技能", "3", "电子病历系统操作", "医疗/健康"},
            {"医疗器械", "专业技能", "3", "医疗设备操作与维护", "医疗/健康"},
            {"公共卫生", "核心技能", "3", "疾病预防与健康促进", "医疗/健康"},
            {"医患沟通", "软技能", "5", "与患者建立信任关系", "医疗/健康"},
            {"急救技能", "核心技能", "4", "紧急情况处理能力", "医疗/健康"},
            {"健康管理", "核心技能", "3", "慢性病管理与健康指导", "医疗/健康"},
            {"科研能力", "软技能", "3", "医学研究与论文撰写", "医疗/健康"},

            // ===== 教育/培训 =====
            {"教学设计", "核心技能", "5", "课程设计与教学规划", "教育/培训"},
            {"课堂管理", "核心技能", "5", "班级管理与学生引导", "教育/培训"},
            {"教育技术", "工具技能", "4", "多媒体教学工具应用", "教育/培训"},
            {"课程开发", "核心技能", "4", "教材编写与课程研发", "教育/培训"},
            {"评估考核", "核心技能", "4", "学生学业评估与反馈", "教育/培训"},
            {"心理咨询", "专业技能", "3", "学生心理辅导", "教育/培训"},
            {"在线教育", "工具技能", "4", "在线教学平台使用", "教育/培训"},
            {"学科知识", "核心技能", "5", "所教学科专业知识", "教育/培训"},
            {"沟通表达", "软技能", "5", "清晰讲解与互动能力", "教育/培训"},
            {"因材施教", "软技能", "4", "个性化教学能力", "教育/培训"},
            {"教研能力", "软技能", "3", "教学研究与改进", "教育/培训"},

            // ===== 市场营销 =====
            {"品牌策划", "核心技能", "5", "品牌定位与传播策略", "市场营销"},
            {"数字营销", "核心技能", "5", "SEO/SEM/社交媒体营销", "市场营销"},
            {"内容营销", "核心技能", "4", "优质内容创作与分发", "市场营销"},
            {"市场调研", "核心技能", "4", "消费者行为分析", "市场营销"},
            {"活动策划", "核心技能", "4", "线上线下营销活动", "市场营销"},
            {"广告投放", "核心技能", "4", "信息流广告与效果优化", "市场营销"},
            {"数据分析", "工具技能", "4", "营销数据追踪与分析", "市场营销"},
            {"Photoshop", "工具技能", "3", "营销素材设计", "市场营销"},
            {"文案写作", "核心技能", "5", "广告文案与推广文案", "市场营销"},
            {"新媒体运营", "核心技能", "4", "微信、抖音等平台运营", "市场营销"},
            {"客户关系", "软技能", "4", "客户维护与关系管理", "市场营销"},
            {"创意思维", "软技能", "4", "创新营销方案设计", "市场营销"},

            // ===== 设计/创意 =====
            {"UI设计", "核心技能", "5", "用户界面视觉设计", "设计/创意"},
            {"UX设计", "核心技能", "5", "用户体验研究与设计", "设计/创意"},
            {"Figma", "工具技能", "5", "主流设计协作工具", "设计/创意"},
            {"Sketch", "工具技能", "4", "Mac端UI设计工具", "设计/创意"},
            {"Photoshop", "工具技能", "5", "图像处理与合成", "设计/创意"},
            {"Illustrator", "工具技能", "4", "矢量图形设计", "设计/创意"},
            {"After Effects", "工具技能", "3", "动效设计与视频后期", "设计/创意"},
            {"3D建模", "工具技能", "3", "三维模型制作", "设计/创意"},
            {"品牌设计", "核心技能", "4", "VI系统与品牌视觉", "设计/创意"},
            {"插画设计", "核心技能", "3", "商业插画与图形创作", "设计/创意"},
            {"交互设计", "核心技能", "4", "交互原型与流程设计", "设计/创意"},
            {"用户研究", "软技能", "4", "用户需求洞察与测试", "设计/创意"},
            {"审美能力", "软技能", "5", "视觉审美与设计感", "设计/创意"},

            // ===== 制造业 =====
            {"质量管理", "核心技能", "5", "ISO体系与质量控制", "制造业"},
            {"生产管理", "核心技能", "5", "生产计划与调度优化", "制造业"},
            {"供应链管理", "核心技能", "4", "采购、物流与库存管理", "制造业"},
            {"精益生产", "核心技能", "4", "消除浪费与效率提升", "制造业"},
            {"工艺工程", "核心技能", "4", "制造工艺设计与改进", "制造业"},
            {"设备维护", "核心技能", "3", "设备保养与故障排除", "制造业"},
            {"安全管理", "核心技能", "4", "生产安全与职业健康", "制造业"},
            {"CAD/CAM", "工具技能", "4", "计算机辅助设计与制造", "制造业"},
            {"ERP系统", "工具技能", "3", "生产资源管理系统", "制造业"},
            {"六西格玛", "专业技能", "3", "质量改进方法论", "制造业"},
            {"团队管理", "软技能", "4", "生产团队管理", "制造业"},

            // ===== 销售/商务 =====
            {"客户开发", "核心技能", "5", "新客户拓展与挖掘", "销售/商务"},
            {"商务谈判", "核心技能", "5", "合同谈判与成交技巧", "销售/商务"},
            {"客户管理", "核心技能", "4", "CRM系统与客户维护", "销售/商务"},
            {"销售策略", "核心技能", "4", "销售计划与目标管理", "销售/商务"},
            {"市场分析", "核心技能", "3", "行业趋势与竞品分析", "销售/商务"},
            {"方案演示", "核心技能", "4", "产品演示与方案讲解", "销售/商务"},
            {"合同管理", "管理技能", "3", "商务合同起草与审核", "销售/商务"},
            {"渠道管理", "核心技能", "3", "销售渠道拓展与维护", "销售/商务"},
            {"抗压能力", "软技能", "5", "承受业绩压力的能力", "销售/商务"},
            {"沟通能力", "软技能", "5", "与客户建立信任关系", "销售/商务"},
            {"自驱力", "软技能", "4", "主动学习与自我激励", "销售/商务"},

            // ===== 人力资源 =====
            {"招聘管理", "核心技能", "5", "人才招聘与选拔", "人力资源"},
            {"培训发展", "核心技能", "4", "员工培训体系建设", "人力资源"},
            {"薪酬福利", "核心技能", "4", "薪酬体系设计与管理", "人力资源"},
            {"绩效管理", "核心技能", "4", "绩效考核与激励机制", "人力资源"},
            {"劳动关系", "核心技能", "4", "劳动法规与员工关系", "人力资源"},
            {"组织发展", "核心技能", "3", "组织架构与人才梯队", "人力资源"},
            {"HR系统", "工具技能", "3", "人力资源信息系统", "人力资源"},
            {"面试技巧", "核心技能", "5", "结构化面试与评估", "人力资源"},
            {"数据分析", "工具技能", "3", "人力资源数据报表", "人力资源"},
            {"沟通协调", "软技能", "5", "跨部门沟通与协调", "人力资源"},
            {"保密意识", "软技能", "4", "人事信息保密管理", "人力资源"},

            // ===== 法律 =====
            {"法律研究", "核心技能", "5", "法规检索与案例分析", "法律"},
            {"合同审查", "核心技能", "5", "合同条款审核与风险识别", "法律"},
            {"诉讼仲裁", "核心技能", "4", "诉讼与仲裁程序", "法律"},
            {"合规管理", "核心技能", "4", "企业合规体系建设", "法律"},
            {"知识产权", "核心技能", "4", "专利、商标与版权保护", "法律"},
            {"公司法务", "核心技能", "4", "公司治理与股权架构", "法律"},
            {"法律文书", "核心技能", "5", "法律意见书与合同起草", "法律"},
            {"谈判能力", "软技能", "4", "纠纷调解与谈判", "法律"},
            {"逻辑思维", "软技能", "5", "严密的法律逻辑推理", "法律"},
            {"保密义务", "软技能", "5", "客户信息保密", "法律"},
        };

        Map<String, Long> skillIdMap = new HashMap<>();
        for (String[] data : industryData) {
            SkillGraph skill = new SkillGraph();
            skill.setName(data[0]);
            skill.setCategory(data[1]);
            skill.setLevel(Integer.parseInt(data[2]));
            skill.setDescription(data[3]);
            skill.setIndustry(data[4]);
            save(skill);
            skillIdMap.put(data[0], skill.getId());
        }

        // 技能关系
        String[][] relations = {
            {"Java", "Spring Boot", "依赖", "9"}, {"Java", "MyBatis", "依赖", "8"},
            {"Spring Boot", "Spring Cloud", "相关", "8"}, {"JavaScript", "Vue.js", "依赖", "9"},
            {"JavaScript", "React", "依赖", "9"}, {"Vue.js", "Element UI", "依赖", "7"},
            {"MySQL", "MyBatis", "相关", "8"}, {"Docker", "Kubernetes", "依赖", "8"},
            {"Linux", "Docker", "相关", "7"}, {"Python", "Django", "依赖", "8"},
            {"Redis", "MySQL", "相关", "6"}, {"Git", "Linux", "相关", "6"},
            {"财务分析", "Excel高级", "依赖", "9"}, {"财务分析", "金融建模", "依赖", "8"},
            {"风险管理", "合规管理", "相关", "8"}, {"会计核算", "税务筹划", "相关", "7"},
            {"投资分析", "金融建模", "依赖", "8"}, {"审计", "合规管理", "相关", "7"},
            {"临床诊断", "药理学", "依赖", "9"}, {"护理技术", "急救技能", "相关", "7"},
            {"医学影像", "临床诊断", "相关", "8"}, {"检验技术", "临床诊断", "相关", "7"},
            {"教学设计", "课程开发", "依赖", "8"}, {"课堂管理", "因材施教", "相关", "7"},
            {"教育技术", "在线教育", "依赖", "8"}, {"学科知识", "教学设计", "依赖", "9"},
            {"品牌策划", "文案写作", "依赖", "8"}, {"数字营销", "数据分析", "依赖", "7"},
            {"内容营销", "新媒体运营", "依赖", "8"}, {"市场调研", "数据分析", "依赖", "7"},
            {"UI设计", "Figma", "依赖", "9"}, {"UX设计", "用户研究", "依赖", "8"},
            {"Photoshop", "Illustrator", "相关", "7"}, {"交互设计", "UX设计", "依赖", "8"},
            {"品牌设计", "Illustrator", "依赖", "7"},
            {"质量管理", "六西格玛", "依赖", "7"}, {"生产管理", "精益生产", "依赖", "8"},
            {"供应链管理", "ERP系统", "依赖", "7"}, {"工艺工程", "CAD/CAM", "依赖", "7"},
            {"客户开发", "商务谈判", "依赖", "8"}, {"客户管理", "CRM系统", "依赖", "7"},
            {"销售策略", "市场分析", "相关", "6"}, {"方案演示", "沟通能力", "依赖", "8"},
            {"招聘管理", "面试技巧", "依赖", "9"}, {"培训发展", "绩效管理", "相关", "6"},
            {"薪酬福利", "劳动关系", "相关", "7"}, {"HR系统", "数据分析", "相关", "6"},
            {"法律研究", "逻辑思维", "依赖", "9"}, {"合同审查", "法律文书", "依赖", "8"},
            {"诉讼仲裁", "谈判能力", "依赖", "7"}, {"知识产权", "合规管理", "相关", "6"},
        };

        for (String[] rel : relations) {
            Long sourceId = skillIdMap.get(rel[0]);
            Long targetId = skillIdMap.get(rel[1]);
            if (sourceId != null && targetId != null) {
                SkillRelation relation = new SkillRelation();
                relation.setSourceId(sourceId);
                relation.setTargetId(targetId);
                relation.setRelationType(rel[2]);
                relation.setWeight(Integer.parseInt(rel[3]));
                relationMapper.insert(relation);
            }
        }
    }

    @Override
    public SkillGraph addSkill(SkillGraph skill) {
        save(skill);
        return skill;
    }

    @Override
    public SkillRelation addRelation(SkillRelation relation) {
        relationMapper.insert(relation);
        return relation;
    }

    @Override
    public Map<String, Object> autoBuildFromRealData() {
        // 获取所有简历和职位数据
        List<Resume> resumes = resumeService.list();
        List<Job> jobs = jobService.list();

        // 统计技能出现频率
        Map<String, Integer> skillFrequency = new HashMap<>();
        Map<String, String> skillIndustryMap = new HashMap<>();
        int totalNewSkills = 0;
        int totalNewFromResumes = 0;
        int totalNewFromJobs = 0;

        // 从简历中提取技能
        for (Resume resume : resumes) {
            // 1. 先自动提取新技能
            int newFromResume = 0;
            if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                newFromResume += extractAndAddSkills(resume.getSkills(), resume.getCategoryId(), "resume");
            }
            if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
                newFromResume += extractAndAddSkills(resume.getExperience(), resume.getCategoryId(), "resume");
            }
            if (resume.getSelfIntroduction() != null && !resume.getSelfIntroduction().isEmpty()) {
                newFromResume += extractAndAddSkills(resume.getSelfIntroduction(), resume.getCategoryId(), "resume");
            }
            totalNewFromResumes += newFromResume;

            // 2. 提取已有技能统计频率
            if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                try {
                    String skillsStr = resume.getSkills();
                    String[] skills = skillsStr.replace("[", "").replace("]", "")
                            .replace("\"", "").split("[,，、;；\\s]+");
                    for (String skill : skills) {
                        skill = skill.trim();
                        if (!skill.isEmpty()) {
                            skillFrequency.merge(skill.toLowerCase(), 1, Integer::sum);
                            if (resume.getCategoryId() != null) {
                                skillIndustryMap.putIfAbsent(skill.toLowerCase(), 
                                    getIndustryByCategoryId(resume.getCategoryId()));
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }

        // 从职位要求中提取技能
        for (Job job : jobs) {
            // 1. 先自动提取新技能
            int newFromJob = 0;
            String jobText = "";
            if (job.getTitle() != null) jobText += job.getTitle() + " ";
            if (job.getDescription() != null) jobText += job.getDescription() + " ";
            if (job.getRequirements() != null) jobText += job.getRequirements();
            
            newFromJob += extractAndAddSkills(jobText, job.getCategoryId(), "job");
            totalNewFromJobs += newFromJob;

            // 2. 提取已有技能统计频率
            if (job.getRequirements() != null && !job.getRequirements().isEmpty()) {
                String reqLower = job.getRequirements().toLowerCase();
                // 匹配已知技能关键词
                for (String keyword : INDUSTRY_KEYWORDS.values().stream()
                        .flatMap(Arrays::stream).toArray(String[]::new)) {
                    if (reqLower.contains(keyword.toLowerCase())) {
                        skillFrequency.merge(keyword.toLowerCase(), 1, Integer::sum);
                        if (job.getCategoryId() != null) {
                            skillIndustryMap.putIfAbsent(keyword.toLowerCase(),
                                getIndustryByCategoryId(job.getCategoryId()));
                        }
                    }
                }
            }
        }

        totalNewSkills = totalNewFromResumes + totalNewFromJobs;

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("totalResumes", resumes.size());
        result.put("totalJobs", jobs.size());
        result.put("uniqueSkills", skillFrequency.size());
        result.put("topSkills", skillFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toList()));
        result.put("industryDistribution", skillIndustryMap.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.counting())));
        result.put("newSkillsAdded", totalNewSkills);
        result.put("newFromResumes", totalNewFromResumes);
        result.put("newFromJobs", totalNewFromJobs);

        return result;
    }

    /**
     * 根据分类ID获取行业名称
     */
    private String getIndustryByCategoryId(Long categoryId) {
        if (categoryId == null) return "IT/互联网";
        try {
            com.smartrecruitment.entity.JobCategory category = jobCategoryMapper.selectById(categoryId);
            if (category != null && category.getName() != null) {
                return category.getName();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return "IT/互联网";
    }

    /**
     * 从文本中提取技能关键词并自动添加到图谱
     * @param text 要分析的文本（简历技能或职位要求）
     * @param categoryId 分类ID，用于确定行业
     * @param sourceType 来源类型：resume 或 job
     * @return 新添加的技能数量
     */
    private int extractAndAddSkills(String text, Long categoryId, String sourceType) {
        if (text == null || text.isEmpty()) return 0;

        int addedCount = 0;
        String industry = getIndustryByCategoryId(categoryId);

        // 获取所有已知技能
        List<SkillGraph> existingSkills = getAllSkills();
        Set<String> existingSkillNames = existingSkills.stream()
                .map(s -> s.getName().toLowerCase())
                .collect(Collectors.toSet());

        // 收集待添加的技能（避免重复）
        Map<String, String[]> skillsToAdd = new LinkedHashMap<>();

        // 解析技能列表：优先按JSON数组解析，否则按逗号/顿号/分号拆分
        List<String> parsedSkills = new ArrayList<>();
        String trimmed = text.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            // JSON数组格式：拆分每个元素
            String inner = trimmed.substring(1, trimmed.length() - 1);
            for (String part : inner.split(",")) {
                String skill = part.trim()
                        .replaceAll("^\"|\"$", "")  // 去掉首尾引号
                        .replaceAll("^'|'$", "")
                        .trim();
                if (!skill.isEmpty()) {
                    parsedSkills.add(skill);
                }
            }
        } else {
            // 非JSON：按逗号、顿号、分号拆分
            for (String part : text.split("[,，、;；]+")) {
                String skill = part.trim();
                if (!skill.isEmpty() && skill.length() <= 30) { // 过滤掉长句子
                    parsedSkills.add(skill);
                }
            }
        }

        for (String skill : parsedSkills) {
            String skillLower = skill.toLowerCase();
            if (!existingSkillNames.contains(skillLower) && !skillsToAdd.containsKey(skillLower)) {
                skillsToAdd.put(skillLower, new String[]{skill, getIndustryByCategoryId(categoryId)});
            }
        }
        
        // 然后再匹配行业关键词库（补充遗漏的技能）
        for (Map.Entry<String, String[]> entry : INDUSTRY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                // 使用正则表达式进行精确匹配（单词边界）
                String regex = "(?i)\\b" + Pattern.quote(keyword) + "\\b";
                // 对于中文关键词，使用完整词匹配（不拆分）
                if (keyword.matches(".*[\\u4e00-\\u9fa5].*")) {
                    // 检查文本中是否包含该完整关键词
                    if (text.contains(keyword)) {
                        String skillKey = keyword.toLowerCase();
                        if (!existingSkillNames.contains(skillKey) && !skillsToAdd.containsKey(skillKey)) {
                            skillsToAdd.put(skillKey, new String[]{keyword, entry.getKey()});
                        }
                    }
                } else {
                    // 英文关键词使用单词边界匹配
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(text);
                    
                    if (matcher.find()) {
                        String skillKey = keyword.toLowerCase();
                        if (!existingSkillNames.contains(skillKey) && !skillsToAdd.containsKey(skillKey)) {
                            skillsToAdd.put(skillKey, new String[]{keyword, entry.getKey()});
                        }
                    }
                }
            }
        }
        
        // 批量添加新技能
        for (Map.Entry<String, String[]> entry : skillsToAdd.entrySet()) {
            String keyword = entry.getValue()[0];
            String matchedIndustry = entry.getValue()[1];
            
            // 根据技能类型自动分类
            String category = categorizeSkill(keyword, matchedIndustry);
            
            SkillGraph newSkill = new SkillGraph();
            newSkill.setName(keyword);
            newSkill.setCategory(category);
            newSkill.setLevel(calculateSkillLevel(keyword, sourceType));
            newSkill.setIndustry(matchedIndustry);
            newSkill.setDescription("从" + ("resume".equals(sourceType) ? "简历" : "职位") + "中自动提取");
            save(newSkill);
            existingSkillNames.add(keyword.toLowerCase());
            addedCount++;
        }
        
        return addedCount;
    }

    @Override
    public int syncSkillsToGraph(String skillsJson, Long categoryId) {
        if (skillsJson == null || skillsJson.isEmpty() || "[]".equals(skillsJson)) return 0;
        return extractAndAddSkills(skillsJson, categoryId, "resume");
    }

    /**
     * 根据技能名称自动分类
     */
    private String categorizeSkill(String skillName, String industry) {
        String lower = skillName.toLowerCase();
        
        // 编程语言
        if (lower.matches(".*(java|python|javascript|typescript|go|c\\+\\+|rust|php|swift|kotlin|scala|ruby).*")) {
            return "编程语言";
        }
        // 前端框架
        if (lower.matches(".*(vue|react|angular|jquery|webpack|html|css).*")) {
            return "前端框架";
        }
        // 后端框架
        if (lower.matches(".*(spring|django|flask|express|mybatis|hibernate).*")) {
            return "后端框架";
        }
        // 数据库
        if (lower.matches(".*(mysql|redis|mongodb|postgresql|oracle|elasticsearch).*")) {
            return "数据库";
        }
        // 开发工具
        if (lower.matches(".*(docker|kubernetes|git|jenkins|linux|nginx|maven).*")) {
            return "开发工具";
        }
        // 设计工具
        if (lower.matches(".*(figma|sketch|photoshop|illustrator|after effects|blender).*")) {
            return "设计工具";
        }
        // 办公软件
        if (lower.matches(".*(excel|powerpoint|word|sap|erp).*")) {
            return "办公软件";
        }
        // 软技能（沟通能力、团队协作等）
        if (lower.matches(".*(沟通|协作|团队|领导力|抗压|学习).*")) {
            return "软技能";
        }
        
        // 默认分类
        if (industry.contains("IT") || industry.contains("互联网")) {
            return "技术技能";
        }
        return "核心技能";
    }
    
    /**
     * 根据技能出现情况计算技能等级（1-5）
     */
    private int calculateSkillLevel(String skillName, String sourceType) {
        // 从职位中提取的技能，默认等级较高
        if ("job".equals(sourceType)) {
            return 4;
        }
        // 从简历中提取的技能，默认等级中等
        return 3;
    }

    private int getCategoryIndex(String category, List<String> categories) {
        int index = categories.indexOf(category);
        return index >= 0 ? index : categories.size() - 1;
    }

    // ==================== 技能推荐与差距分析 ====================

    @Override
    public List<Map<String, Object>> recommendSkills(String currentSkills, String targetJobRequirements) {
        Set<String> current = parseSkillString(currentSkills);
        Set<String> required = extractSkillsFromText(targetJobRequirements);

        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 找出缺失的技能
        Set<String> missing = new HashSet<>(required);
        missing.removeAll(current);

        // 找出已掌握的技能
        Set<String> mastered = new HashSet<>(required);
        mastered.retainAll(current);

        // 为每个缺失技能计算推荐优先级
        for (String skill : missing) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("skill", skill);
            rec.put("status", "missing");

            // 根据技能在职位要求中的重要性设置优先级
            SkillGraph sg = lambdaQuery().eq(SkillGraph::getName, skill).one();
            if (sg != null) {
                rec.put("category", sg.getCategory());
                rec.put("industry", sg.getIndustry());
                // 等级越高说明越重要
                int priority = sg.getLevel() != null ? sg.getLevel() : 3;
                rec.put("priority", priority >= 4 ? "高" : priority >= 2 ? "中" : "低");
                rec.put("priorityValue", priority);
            } else {
                rec.put("category", "未知");
                rec.put("priority", "中");
                rec.put("priorityValue", 2);
            }

            // 查找相关已掌握技能（作为学习基础）
            List<String> relatedMastered = findRelatedSkills(skill, mastered);
            if (!relatedMastered.isEmpty()) {
                rec.put("relatedSkills", relatedMastered);
                rec.put("learningTip", "您已掌握 " + String.join("、", relatedMastered) + "，学习 " + skill + " 会更容易");
            }

            recommendations.add(rec);
        }

        // 已掌握的技能也列出，用于展示优势
        for (String skill : mastered) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("skill", skill);
            rec.put("status", "mastered");
            rec.put("priority", "-");
            rec.put("priorityValue", 0);
            recommendations.add(rec);
        }

        // 按优先级排序（高 -> 中 -> 低）
        recommendations.sort((a, b) -> {
            int va = (int) a.getOrDefault("priorityValue", 0);
            int vb = (int) b.getOrDefault("priorityValue", 0);
            return Integer.compare(vb, va);
        });

        return recommendations;
    }

    @Override
    public Map<String, Object> analyzeSkillGap(String currentSkills, String targetRequirements) {
        Set<String> current = parseSkillString(currentSkills);
        Set<String> required = extractSkillsFromText(targetRequirements);

        Set<String> mastered = new HashSet<>(required);
        mastered.retainAll(current);

        Set<String> missing = new HashSet<>(required);
        missing.removeAll(current);

        // 部分掌握：通过同义词/别名匹配
        Set<String> partial = new HashSet<>();
        for (String reqSkill : missing) {
            for (String curSkill : current) {
                if (isSimilarSkill(curSkill, reqSkill)) {
                    partial.add(reqSkill);
                    break;
                }
            }
        }
        missing.removeAll(partial);

        int total = required.isEmpty() ? 1 : required.size();
        double matchRate = (double) (mastered.size() + partial.size() * 0.5) / total * 100;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequired", required.size());
        result.put("mastered", new ArrayList<>(mastered));
        result.put("masteredCount", mastered.size());
        result.put("partial", new ArrayList<>(partial));
        result.put("partialCount", partial.size());
        result.put("missing", new ArrayList<>(missing));
        result.put("missingCount", missing.size());
        result.put("matchRate", Double.parseDouble(String.format("%.1f", matchRate)));
        result.put("matchLevel", matchRate >= 80 ? "高度匹配" : matchRate >= 60 ? "基本匹配" : matchRate >= 40 ? "部分匹配" : "差距较大");

        return result;
    }

    @Override
    public List<SkillGraph> getLearningPath(String fromSkill, String toSkill) {
        // 使用BFS查找最短学习路径
        SkillGraph start = lambdaQuery().eq(SkillGraph::getName, fromSkill).one();
        SkillGraph end = lambdaQuery().eq(SkillGraph::getName, toSkill).one();

        if (start == null || end == null) {
            return Collections.emptyList();
        }

        // BFS
        Map<Long, Long> parentMap = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(start.getId());
        visited.add(start.getId());
        boolean found = false;

        while (!queue.isEmpty() && !found) {
            Long currentId = queue.poll();
            List<SkillRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<SkillRelation>().eq(SkillRelation::getSourceId, currentId)
            );

            for (SkillRelation rel : relations) {
                Long targetId = rel.getTargetId();
                if (targetId != null && !visited.contains(targetId)) {
                    visited.add(targetId);
                    parentMap.put(targetId, currentId);
                    if (targetId.equals(end.getId())) {
                        found = true;
                        break;
                    }
                    queue.add(targetId);
                }
            }
        }

        if (!found) {
            return Collections.emptyList();
        }

        // 回溯路径
        List<SkillGraph> path = new ArrayList<>();
        Long current = end.getId();
        while (current != null) {
            SkillGraph node = getById(current);
            if (node != null) path.add(0, node);
            current = parentMap.get(current);
        }

        return path;
    }

    // ==================== 辅助方法 ====================

    private Set<String> parseSkillString(String skills) {
        Set<String> result = new HashSet<>();
        if (skills == null || skills.isEmpty()) return result;
        String[] parts = skills.replace("[", "").replace("]", "")
                .replace("\"", "").replace("'", "")
                .split("[,，、;；\\s]+");
        for (String s : parts) {
            String trimmed = s.trim().toLowerCase();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private Set<String> extractSkillsFromText(String text) {
        Set<String> skills = new HashSet<>();
        if (text == null || text.isEmpty()) return skills;
        String textLower = text.toLowerCase();

        // 从技能图谱中匹配
        List<SkillGraph> allSkills = getAllSkills();
        for (SkillGraph sg : allSkills) {
            if (textLower.contains(sg.getName().toLowerCase())) {
                skills.add(sg.getName().toLowerCase());
            }
        }
        return skills;
    }

    private List<String> findRelatedSkills(String targetSkill, Set<String> candidateSkills) {
        List<String> related = new ArrayList<>();
        SkillGraph target = lambdaQuery().eq(SkillGraph::getName, targetSkill).one();
        if (target == null) return related;

        // 查找与目标技能同类别的已掌握技能
        for (String candidate : candidateSkills) {
            SkillGraph cand = lambdaQuery().eq(SkillGraph::getName, candidate).one();
            if (cand != null && target.getCategory() != null
                    && target.getCategory().equals(cand.getCategory())) {
                related.add(candidate);
            }
        }
        return related;
    }

    private boolean isSimilarSkill(String a, String b) {
        if (a.equals(b)) return true;
        // 简单的相似度判断：包含关系或编辑距离
        if (a.contains(b) || b.contains(a)) return true;
        // 同类别技能视为部分匹配
        SkillGraph sgA = lambdaQuery().eq(SkillGraph::getName, a).one();
        SkillGraph sgB = lambdaQuery().eq(SkillGraph::getName, b).one();
        if (sgA != null && sgB != null
                && sgA.getCategory() != null
                && sgA.getCategory().equals(sgB.getCategory())) {
            return true;
        }
        return false;
    }
}
