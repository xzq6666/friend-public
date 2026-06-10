package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrecruitment.entity.Job;
import com.smartrecruitment.entity.JobKeyword;
import com.smartrecruitment.entity.MatchRecord;
import com.smartrecruitment.entity.Resume;
import com.smartrecruitment.mapper.MatchRecordMapper;
import com.smartrecruitment.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 匹配记录服务实现 - v7
 *
 * 核心改进：从累加制改为乘数制
 * 总分 = 技能归一化(0~100) x 行业系数 x 经验系数 x 学历系数 + 微调 + 加分
 * 技能不匹配直接导致总分极低，其他维度无法弥补
 */
@Service
public class MatchRecordServiceImpl extends ServiceImpl<MatchRecordMapper, MatchRecord> implements MatchRecordService {

    private static final Logger log = LoggerFactory.getLogger(MatchRecordServiceImpl.class);

    /** 推荐结果缓存 TTL（分钟） */
    private static final int RECOMMEND_CACHE_TTL = 30;

    @Autowired
    private ResumeService resumeService;
    @Autowired
    private JobService jobService;
    @Autowired
    private AsyncMatchService asyncMatchService;
    @Autowired
    private JobKeywordService jobKeywordService;
    @Autowired
    private AIService aiService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 缓存的职位关键词列表，避免 N+1 查询 */
    private volatile List<JobKeyword> cachedJobKeywords;
    private volatile long keywordsCacheTime = 0;
    private static final long KEYWORDS_CACHE_TTL = 5 * 60 * 1000; // 5分钟

    // ==================== 技能标准化 ====================
    private static final Map<String, String> SKILL_NORMALIZE = new HashMap<>();
    static {
        SKILL_NORMALIZE.put("vue.js", "vue"); SKILL_NORMALIZE.put("vuejs", "vue");
        SKILL_NORMALIZE.put("vue2", "vue"); SKILL_NORMALIZE.put("vue3", "vue");
        SKILL_NORMALIZE.put("react.js", "react"); SKILL_NORMALIZE.put("reactjs", "react");
        SKILL_NORMALIZE.put("angular.js", "angular"); SKILL_NORMALIZE.put("angularjs", "angular");
        SKILL_NORMALIZE.put("node.js", "node"); SKILL_NORMALIZE.put("nodejs", "node");
        SKILL_NORMALIZE.put("spring boot", "springboot"); SKILL_NORMALIZE.put("spring-boot", "springboot");
        SKILL_NORMALIZE.put("spring cloud", "springcloud"); SKILL_NORMALIZE.put("spring-cloud", "springcloud");
        SKILL_NORMALIZE.put("kubernetes", "k8s"); SKILL_NORMALIZE.put("postgresql", "postgres");
        SKILL_NORMALIZE.put("c++", "plusplus"); SKILL_NORMALIZE.put("c#", "csharp");
        SKILL_NORMALIZE.put(".net", "dotnet"); SKILL_NORMALIZE.put("asp.net", "aspdotnet");
        SKILL_NORMALIZE.put("elasticsearch", "es");
        SKILL_NORMALIZE.put("golang", "go");
        SKILL_NORMALIZE.put("javascript", "js"); SKILL_NORMALIZE.put("typescript", "ts");
        // 医疗
        SKILL_NORMALIZE.put("护理学", "护理"); SKILL_NORMALIZE.put("临床护理", "护理");
        SKILL_NORMALIZE.put("心血管内科", "心血管"); SKILL_NORMALIZE.put("心内科", "心血管");
        SKILL_NORMALIZE.put("医学影像", "影像"); SKILL_NORMALIZE.put("影像诊断", "影像");
        SKILL_NORMALIZE.put("超声", "b超"); SKILL_NORMALIZE.put("核磁共振", "mri");
        SKILL_NORMALIZE.put("ct扫描", "ct"); SKILL_NORMALIZE.put("医学检验", "检验");
        SKILL_NORMALIZE.put("康复治疗", "康复"); SKILL_NORMALIZE.put("药理学", "药学");
        SKILL_NORMALIZE.put("住院医师规范化培训", "规培");
        SKILL_NORMALIZE.put("医师资格", "执业医师"); SKILL_NORMALIZE.put("药师资格", "执业药师");
        // 教育
        SKILL_NORMALIZE.put("教师资格", "教师资格证"); SKILL_NORMALIZE.put("英语专业八级", "英语专八");
        SKILL_NORMALIZE.put("cet-6", "英语六级"); SKILL_NORMALIZE.put("cet6", "英语六级");
        SKILL_NORMALIZE.put("cet-4", "英语四级"); SKILL_NORMALIZE.put("cet4", "英语四级");
        // 金融
        SKILL_NORMALIZE.put("注册会计师", "cpa"); SKILL_NORMALIZE.put("特许金融分析师", "cfa");
    }

    // ==================== 别名组 ====================
    private static final List<Set<String>> ALIAS_GROUPS = Arrays.asList(
        Set.of("go", "golang"), Set.of("k8s", "kubernetes"),
        Set.of("postgres", "postgresql"), Set.of("springboot", "spring boot"),
        Set.of("springcloud", "spring cloud"), Set.of("js", "javascript"),
        Set.of("ts", "typescript"), Set.of("vue", "vue.js", "vuejs"),
        Set.of("react", "react.js"), Set.of("plusplus", "c++"),
        Set.of("csharp", "c#"), Set.of("dotnet", ".net"),
        Set.of("es", "elasticsearch"), Set.of("aliyun", "阿里云"),
        Set.of("tencentcloud", "腾讯云"),
        // 医疗别名
        Set.of("护理", "护理学", "临床护理"), Set.of("心内科", "心血管内科", "心血管"),
        Set.of("执业医师", "医师资格"), Set.of("执业药师", "药师资格"),
        Set.of("b超", "超声"), Set.of("ct", "ct扫描"), Set.of("mri", "核磁共振"),
        Set.of("影像", "医学影像", "影像诊断"), Set.of("检验", "医学检验"),
        Set.of("康复", "康复治疗"), Set.of("药剂", "药学", "药理学"),
        Set.of("规培", "住院医师规范化培训"), Set.of("主治", "主治医师"),
        Set.of("副主任", "副主任医师"), Set.of("主任", "主任医师"),
        // 教育别名
        Set.of("教师资格证", "教师资格"), Set.of("英语专八", "专八", "英语专业八级"),
        Set.of("英语六级", "cet6", "cet-6"), Set.of("英语四级", "cet4", "cet-4"),
        // 金融别名
        Set.of("cpa", "注册会计师"), Set.of("cfa", "特许金融分析师"),
        Set.of("acca", "国际注册会计师")
    );

    // ==================== 行业映射 ====================
    // 1=IT/互联网 2=金融/会计 3=教育 4=医疗 5=制造 6=销售 7=行政 8=建筑 9=传媒 10=服务
    private static final Map<Long, Set<Long>> RELATED_CATEGORIES = new HashMap<>();
    static {
        RELATED_CATEGORIES.put(1L, Set.of(1L, 2L, 3L, 9L));
        RELATED_CATEGORIES.put(2L, Set.of(1L, 2L, 6L));
        RELATED_CATEGORIES.put(3L, Set.of(1L, 3L, 10L));
        RELATED_CATEGORIES.put(4L, Set.of(4L, 3L, 10L)); // 医疗关联教育（医学教育）、服务（护理服务）
        RELATED_CATEGORIES.put(5L, Set.of(5L, 8L));
        RELATED_CATEGORIES.put(6L, Set.of(2L, 6L, 9L));
        RELATED_CATEGORIES.put(7L, Set.of(7L, 10L));
        RELATED_CATEGORIES.put(8L, Set.of(5L, 8L));
        RELATED_CATEGORIES.put(9L, Set.of(1L, 6L, 9L));
        RELATED_CATEGORIES.put(10L, Set.of(7L, 10L, 3L, 4L));
    }

    // ==================== 行业关键词 ====================
    private static final Map<String, Set<String>> INDUSTRY_KEYWORDS = new HashMap<>();
    static {
        INDUSTRY_KEYWORDS.put("IT", Set.of("java","python","spring","javascript","前端","后端","开发","编程","软件","互联网","数据库","微服务","分布式","高并发","算法","程序","代码","devops","docker","linux","sql","redis","mysql","架构","系统","api","服务器","git","nginx","kafka","elasticsearch","mongodb","rabbitmq","vue","react","angular","node","typescript","go","rust","swift","kotlin","flutter","android","ios","机器学习","深度学习","人工智能","大数据","hadoop","spark","flink","数据仓库","区块链","solidity","网络安全","渗透测试"));
        INDUSTRY_KEYWORDS.put("FINANCE", Set.of("金融","财务","会计","银行","证券","投资","基金","保险","审计","税务","理财","cfa","cpa","核算","报表","风控","信贷","资管","信托","期货","外汇","精算"));
        INDUSTRY_KEYWORDS.put("EDUCATION", Set.of("教育","培训","教学","课程","学生","教师","讲师","授课","辅导","学校","大学","在线教育","家教","幼教","k12","留学","考研","考试"));
        INDUSTRY_KEYWORDS.put("MEDICAL", Set.of("医生","护士","医疗","临床","诊断","治疗","手术","患者","医院","医药","处方","规培","执业","内科","外科","儿科","妇科","心血管","骨科","肿瘤","门诊","病房","急诊","麻醉","影像","检验","药剂","康复","护理"));
        INDUSTRY_KEYWORDS.put("MANUFACTURING", Set.of("制造","工程","机械","生产","工艺","质量","车间","流水线","自动化","电气","模具","加工","装配","焊接","铸造","热处理","cnc","plm","mes","erp"));
        INDUSTRY_KEYWORDS.put("SALES", Set.of("销售","市场","营销","客户","渠道","推广","品牌","商务","业务","业绩","kpi","转化","获客","bd","地推","电销","网销","门店","导购","店长"));
        INDUSTRY_KEYWORDS.put("ADMIN", Set.of("行政","人事","hr","招聘","薪酬","考勤","前台","助理","文员","秘书","后勤","办公","档案","会议","接待","采购","仓管","物流","供应链"));
        INDUSTRY_KEYWORDS.put("CONSTRUCTION", Set.of("建筑","房地产","施工","设计","装修","物业","土木","结构","造价","监理","楼盘","土建","暖通","给排水","电气","消防","幕墙","园林","市政","公路","桥梁","隧道"));
        INDUSTRY_KEYWORDS.put("MEDIA", Set.of("传媒","广告","设计","创意","编辑","记者","摄影","视频","新媒体","运营","内容","ui","平面","动画","游戏","原画","建模","特效","剪辑","播音","主持","公关","品牌","策划"));
        INDUSTRY_KEYWORDS.put("SERVICE", Set.of("服务","酒店","餐饮","旅游","客服","接待","护理","美容","健身","快递","物流","仓储","配送","司机","保安","保洁","月嫂","保姆","维修","安装"));
    }

    // v9 优化权重分配：技能50% + 行业15% + 经验20% + 学历10% = 95基础分 + 薪资微调(-3~+2) + 加分(0~10)
    // 优化理由：技能是核心竞争力，经验比学历更重要，行业相关度适中即可
    private static final double MAX_SKILL = 50.0;
    private static final double MAX_EXPERIENCE = 20.0;
    private static final double MAX_EDUCATION = 10.0;
    private static final double MAX_INDUSTRY = 15.0;
    private static final double MAX_SALARY = 8.0;
    private static final double MAX_LOCATION = 5.0;
    private static final double MAX_BONUS = 10.0;
    private static final double HARD_FILTER_CAP = 20.0;
    private static final double ZERO_SKILL_CAP = 35.0; // 技能覆盖率0%时总分上限（降低）
    private static final double CROSS_INDUSTRY_SKILL_PENALTY = 0.30; // 跨行业时技能分惩罚加重

    private static final Set<String> CITIES = Set.of(
        "北京","上海","广州","深圳","杭州","成都","武汉","南京","西安","重庆",
        "苏州","天津","长沙","郑州","合肥","福州","厦门","青岛","大连","昆明",
        "济南","沈阳","哈尔滨","长春","石家庄","太原","南昌","贵阳","南宁","海口",
        "兰州","西宁","银川","乌鲁木齐","拉萨","呼和浩特","东莞","佛山","珠海","中山"
    );

    // ==================== CRUD ====================

    @Override
    public MatchRecord createMatchRecord(MatchRecord matchRecord) {
        save(matchRecord);
        return matchRecord;
    }

    /**
     * 批量保存匹配记录（用于统计趋势）
     */
    private void saveMatchRecords(Resume resume, List<Map<String, Object>> recommendations) {
        try {
            List<MatchRecord> records = new ArrayList<>();
            for (Map<String, Object> rec : recommendations) {
                Object jobObj = rec.get("job");
                Job job = null;
                if (jobObj instanceof Job) {
                    job = (Job) jobObj;
                } else if (jobObj instanceof Map) {
                    job = objectMapper.convertValue(jobObj, Job.class);
                }
                if (job == null) continue;

                BigDecimal score = (BigDecimal) rec.getOrDefault("matchScore", BigDecimal.ZERO);
                String suggestion = (String) rec.getOrDefault("aiSuggestion", "");

                MatchRecord record = new MatchRecord();
                record.setResumeId(resume.getId());
                record.setJobId(job.getId());
                record.setMatchScore(score);
                record.setAiSuggestion(suggestion);
                record.setStatus(0);
                records.add(record);
            }
            if (!records.isEmpty()) {
                saveBatch(records);
                log.info("保存匹配记录: resumeId={}, 记录数={}", resume.getId(), records.size());
            }
        } catch (Exception e) {
            log.error("保存匹配记录失败: {}", e.getMessage());
        }
    }

    /**
     * 批量保存候选人匹配记录（企业端匹配）
     */
    private void saveCandidateMatchRecords(Job job, List<Map<String, Object>> recommendations) {
        try {
            List<MatchRecord> records = new ArrayList<>();
            for (Map<String, Object> rec : recommendations) {
                Object resumeObj = rec.get("resume");
                Resume resume = null;
                if (resumeObj instanceof Resume) {
                    resume = (Resume) resumeObj;
                } else if (resumeObj instanceof Map) {
                    resume = objectMapper.convertValue(resumeObj, Resume.class);
                }
                if (resume == null) continue;

                BigDecimal score = (BigDecimal) rec.getOrDefault("matchScore", BigDecimal.ZERO);
                String suggestion = (String) rec.getOrDefault("aiSuggestion", "");

                MatchRecord record = new MatchRecord();
                record.setResumeId(resume.getId());
                record.setJobId(job.getId());
                record.setMatchScore(score);
                record.setAiSuggestion(suggestion);
                record.setStatus(0);
                records.add(record);
            }
            if (!records.isEmpty()) {
                saveBatch(records);
                log.info("保存候选人匹配记录: jobId={}, 记录数={}", job.getId(), records.size());
            }
        } catch (Exception e) {
            log.error("保存候选人匹配记录失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getMatchRecordsWithJob(Long resumeId) {
        return baseMapper.getMatchRecordsWithJob(resumeId);
    }

    @Override
    public List<Map<String, Object>> getMatchRecordsWithResume(Long jobId) {
        return baseMapper.getMatchRecordsWithResume(jobId);
    }

    @Override
    public IPage<MatchRecord> getMatchRecordPage(int page, int size, Long resumeId, Long jobId) {
        IPage<MatchRecord> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<MatchRecord> wrapper = new LambdaQueryWrapper<>();
        if (resumeId != null) wrapper.eq(MatchRecord::getResumeId, resumeId);
        if (jobId != null) wrapper.eq(MatchRecord::getJobId, jobId);
        wrapper.orderByDesc(MatchRecord::getMatchScore);
        return page(pageResult, wrapper);
    }

    @Override
    public boolean updateMatchStatus(Long id, Integer status) {
        MatchRecord record = getById(id);
        if (record != null) {
            record.setStatus(status);
            return updateById(record);
        }
        return false;
    }

    /** 预筛选候选池大小上限 */
    private static final int PRE_FILTER_POOL_SIZE = 150;
    
    /** v10新增：性能监控 - 慢查询阈值（毫秒） */
    private static final long SLOW_QUERY_THRESHOLD_MS = 500;
    
    /** v10新增：缓存预热标记 */
    private volatile boolean cacheWarmed = false;
    
    /** v10新增：技能权重缓存（避免重复计算） */
    private final Map<String, Map<String, Double>> skillWeightsCache = new HashMap<>();
    private static final int SKILL_WEIGHTS_CACHE_MAX_SIZE = 1000;

    // ==================== 推荐引擎 ====================

    @Override
    public List<Map<String, Object>> recommendJobsForResume(Long resumeId, int limit) {
        long startTime = System.currentTimeMillis();
        
        // 缓存key包含limit参数，避免不同limit请求返回错误数量
        String cacheKey = "match:recommend:resume:" + resumeId + ":limit" + limit;

        // 1. 先查 Redis 缓存
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("推荐结果命中缓存: resumeId={}", resumeId);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cachedList = (List<Map<String, Object>>) cached;
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > SLOW_QUERY_THRESHOLD_MS) {
                    log.warn("慢查询警告: recommendJobsForResume resumeId={} 耗时={}ms (从缓存)", resumeId, elapsed);
                }
                return cachedList.subList(0, Math.min(limit, cachedList.size()));
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，降级为实时计算: {}", e.getMessage());
        }

        // 2. 缓存未命中，实时计算
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) return new ArrayList<>();

        // 预筛选：按行业相关性 + 最新发布排序，限制候选池大小
        List<Job> jobs = preFilterJobs(resume);
        if (jobs.isEmpty()) return new ArrayList<>();
        long filterElapsed = System.currentTimeMillis() - startTime;
        log.info("预筛选加载职位数: {}, 耗时: {}ms", jobs.size(), filterElapsed);

        List<Map<String, Object>> recommendations = buildRecommendations(resume, jobs, limit);

        // 保存匹配记录到数据库（用于统计趋势）
        saveMatchRecords(resume, recommendations);

        // 3. 转为可缓存的纯 Map 结构（避免实体对象中 LocalDateTime 序列化问题和循环引用）
        List<Map<String, Object>> cacheableList = toCacheableJobList(recommendations);

        // 4. 写入 Redis 缓存（异步 AI 增强会在后台更新缓存）
        try {
            redisTemplate.opsForValue().set(cacheKey, cacheableList, RECOMMEND_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        log.info("推荐计算完成: resumeId={}, 结果数={}, 总耗时: {}ms", resumeId, cacheableList.size(), totalElapsed);

        // v10新增：慢查询告警
        if (totalElapsed > SLOW_QUERY_THRESHOLD_MS) {
            log.warn("慢查询警告: recommendJobsForResume resumeId={} 耗时={}ms (实时计算)", resumeId, totalElapsed);
        }

        // 返回安全的纯 Map 列表（不包含实体对象，避免 Jackson 序列化循环引用）
        return cacheableList;
    }

    @Override
    public List<Map<String, Object>> recommendCandidatesForJob(Long jobId, int limit) {
        String cacheKey = "match:recommend:job:" + jobId;

        // 1. 先查 Redis 缓存
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("推荐结果命中缓存: jobId={}", jobId);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cachedList = (List<Map<String, Object>>) cached;
                return cachedList.subList(0, Math.min(limit, cachedList.size()));
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，降级为实时计算: {}", e.getMessage());
        }

        // 2. 缓存未命中，实时计算
        long startTime = System.currentTimeMillis();
        Job job = jobService.findById(jobId);
        if (job == null) return new ArrayList<>();

        // 预筛选：按行业匹配 + 最新创建排序，限制候选池大小
        List<Resume> resumes = preFilterResumes(job);
        if (resumes.isEmpty()) return new ArrayList<>();
        log.info("预筛选加载简历数: {}, 耗时: {}ms", resumes.size(), System.currentTimeMillis() - startTime);

        List<Map<String, Object>> recommendations = new ArrayList<>();
        List<Map<String, Object>> needsAiReview = new ArrayList<>();

        for (Resume resume : resumes) {
            Map<String, Object> detail = calculateMatchDetail(resume, job);
            BigDecimal score = new BigDecimal(detail.get("total").toString());
            String suggestion = generateSuggestion(resume, job, score, detail);
            Map<String, Object> rec = buildRecommendationMap(resume, job, score, suggestion, detail);
            if (score.doubleValue() >= 35 && score.doubleValue() < 65) {
                rec.put("needsAiReview", true);
                needsAiReview.add(rec);
            }
            recommendations.add(rec);
        }

        // AI 语义匹配异步执行
        if (!needsAiReview.isEmpty()) {
            asyncMatchService.asyncAiEnhanceForJob(job, needsAiReview);
        }

        sortRecommendations(recommendations);

        // 保存匹配记录到数据库（用于统计趋势）
        saveCandidateMatchRecords(job, recommendations);

        // 3. 转为可缓存的纯 Map 结构（避免实体对象序列化循环引用）
        List<Map<String, Object>> cacheableList = toCacheableCandidateList(recommendations);

        // 4. 写入 Redis 缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, cacheableList, RECOMMEND_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }

        return cacheableList.subList(0, Math.min(limit, cacheableList.size()));
    }

    // ==================== 预筛选（减少候选池，提升性能）====================

    /**
     * 预筛选职位：优先加载同行业/相关行业的职位，再补充其他行业的热门职位
     * 限制候选池大小避免全表扫描
     */
    private List<Job> preFilterJobs(Resume resume) {
        Long resumeCatId = resume.getCategoryId();
        // 如果简历没有行业分类，通过内容推断
        if (resumeCatId == null) {
            resumeCatId = inferCategoryId(resume);
        }

        // 1. 优先加载同行业/相关行业的最新职位（最多100个）
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 1)
                .orderByDesc(Job::getCreateTime)
                .last("LIMIT " + PRE_FILTER_POOL_SIZE);

        // 如果简历有行业分类，优先匹配同行业
        if (resumeCatId != null) {
            Set<Long> catIds = new HashSet<>();
            catIds.add(resumeCatId);
            Set<Long> related = RELATED_CATEGORIES.get(resumeCatId);
            if (related != null) catIds.addAll(related);

            // 先查同行业/相关行业的职位
            wrapper.and(w -> w.in(Job::getCategoryId, catIds).or().isNull(Job::getCategoryId));
        }

        List<Job> jobs = jobService.list(wrapper);

        // 2. 如果同行业职位太少，补充最新职位
        if (jobs.size() < 50) {
            LambdaQueryWrapper<Job> fallback = new LambdaQueryWrapper<Job>()
                    .eq(Job::getStatus, 1)
                    .orderByDesc(Job::getCreateTime)
                    .last("LIMIT " + PRE_FILTER_POOL_SIZE);
            List<Job> allJobs = jobService.list(fallback);
            Set<Long> existingIds = new HashSet<>();
            jobs.forEach(j -> existingIds.add(j.getId()));
            for (Job j : allJobs) {
                if (!existingIds.contains(j.getId())) {
                    jobs.add(j);
                    if (jobs.size() >= PRE_FILTER_POOL_SIZE) break;
                }
            }
        }

        return jobs;
    }

    /**
     * 预筛选简历：优先加载同行业的简历，再补充其他简历
     * 限制候选池大小避免全表扫描
     */
    private List<Resume> preFilterResumes(Job job) {
        Long jobCatId = job.getCategoryId();

        // 1. 优先加载同行业/相关行业的最新简历
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<Resume>()
                .orderByDesc(Resume::getCreateTime)
                .last("LIMIT " + PRE_FILTER_POOL_SIZE);

        if (jobCatId != null) {
            Set<Long> catIds = new HashSet<>();
            catIds.add(jobCatId);
            Set<Long> related = RELATED_CATEGORIES.get(jobCatId);
            if (related != null) catIds.addAll(related);

            wrapper.and(w -> w.in(Resume::getCategoryId, catIds).or().isNull(Resume::getCategoryId));
        }

        List<Resume> resumes = resumeService.list(wrapper);

        // 2. 如果同行业简历太少，补充最新简历
        if (resumes.size() < 50) {
            LambdaQueryWrapper<Resume> fallback = new LambdaQueryWrapper<Resume>()
                    .orderByDesc(Resume::getCreateTime)
                    .last("LIMIT " + PRE_FILTER_POOL_SIZE);
            List<Resume> allResumes = resumeService.list(fallback);
            Set<Long> existingIds = new HashSet<>();
            resumes.forEach(r -> existingIds.add(r.getId()));
            for (Resume r : allResumes) {
                if (!existingIds.contains(r.getId())) {
                    resumes.add(r);
                    if (resumes.size() >= PRE_FILTER_POOL_SIZE) break;
                }
            }
        }

        return resumes;
    }

    private List<Map<String, Object>> buildRecommendations(Resume resume, List<Job> jobs, int limit) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        List<Map<String, Object>> needsAiReview = new ArrayList<>();

        for (Job job : jobs) {
            Map<String, Object> rec = new HashMap<>();
            try {
                Map<String, Object> detail = calculateMatchDetail(resume, job);
                BigDecimal score = new BigDecimal(detail.get("total").toString());
                String suggestion = generateSuggestion(resume, job, score, detail);
                rec.put("matchScore", score);
                rec.put("aiSuggestion", suggestion);
                rec.put("matchDetail", detail);
                if (score.doubleValue() >= 35 && score.doubleValue() < 65) {
                    rec.put("needsAiReview", true);
                    needsAiReview.add(rec);
                }
            } catch (Exception e) {
                rec.put("matchScore", BigDecimal.ZERO);
                rec.put("aiSuggestion", "匹配计算失败");
            }
            rec.put("job", job);
            recommendations.add(rec);
        }

        // AI 语义匹配异步执行，不阻塞主响应
        if (!needsAiReview.isEmpty()) {
            asyncMatchService.asyncAiEnhance(resume, needsAiReview);
        }

        sortRecommendations(recommendations);
        return recommendations.subList(0, Math.min(limit, recommendations.size()));
    }

    private Map<String, Object> buildRecommendationMap(Resume resume, Job job, BigDecimal score, String suggestion, Map<String, Object> detail) {
        Map<String, Object> rec = new HashMap<>();
        rec.put("matchScore", score);
        rec.put("aiSuggestion", suggestion);
        rec.put("matchDetail", detail);
        rec.put("resume", resume);
        return rec;
    }

    @SuppressWarnings("unchecked")
    private void sortRecommendations(List<Map<String, Object>> recommendations) {
        recommendations.sort((a, b) -> {
            double scoreA = calcCompositeScore(a);
            double scoreB = calcCompositeScore(b);
            return Double.compare(scoreB, scoreA);
        });
    }

    @SuppressWarnings("unchecked")
    private double calcCompositeScore(Map<String, Object> rec) {
        BigDecimal matchScore = (BigDecimal) rec.getOrDefault("matchScore", BigDecimal.ZERO);
        double base = matchScore.doubleValue() * 0.7;
        double freshness = 10;
        Object jobObj = rec.get("job");
        Job job = null;
        if (jobObj instanceof Job) {
            job = (Job) jobObj;
        } else if (jobObj instanceof Map) {
            // Redis反序列化后为Map，手动转换
            Map<String, Object> jobMap = (Map<String, Object>) jobObj;
            job = objectMapper.convertValue(jobMap, Job.class);
        }
        if (job != null && job.getCreateTime() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(job.getCreateTime(), LocalDateTime.now());
            if (days <= 3) freshness = 20;
            else if (days <= 7) freshness = 15;
            else if (days <= 14) freshness = 10;
            else if (days <= 30) freshness = 5;
            else freshness = 2;
        }
        double adaptability = 20;
        Map<String, Object> detail = (Map<String, Object>) rec.get("matchDetail");
        if (detail != null && Boolean.TRUE.equals(detail.get("hardFilterBlocked"))) {
            adaptability = 0;
        }
        return base + freshness * 0.15 + adaptability * 0.15;
    }

    // ==================== 核心匹配算法 v8 ====================

    @Override
    public Map<String, Object> calculateMatchDetail(Resume resume, Job job) {
        // 1. 技能匹配（核心，0~45分）
        Map<String, Object> skillResult = calcSkillScoreDetail(resume, job);
        double skillScore = (double) skillResult.get("score");
        int coverage = (int) skillResult.getOrDefault("coverage", 0);

        // 2. 行业得分（0~20分）
        double industryFactor = calcIndustryFactor(resume, job);
        double industryScore = MAX_INDUSTRY * industryFactor;
        boolean crossIndustry = industryFactor <= 0.2;

        // 3. 经验得分（0~18分）
        double experienceFactor = calcExperienceFactor(resume, job);
        double experienceScore = MAX_EXPERIENCE * experienceFactor;

        // 4. 学历得分（0~12分）
        double educationFactor = calcEducationFactor(resume, job);
        double educationScore = MAX_EDUCATION * educationFactor;

        // 5. 跨行业惩罚：技能分大幅缩水
        if (crossIndustry) {
            skillScore = skillScore * CROSS_INDUSTRY_SKILL_PENALTY;
        }

        // 6. 微调
        double salaryAdj = calcSalaryAdjustment(resume, job);
        double locationAdj = calcLocationAdjustment(resume, job);

        // 7. 加分
        Map<String, Object> bonusResult = calcBonusPoints(resume, job, skillResult, educationFactor);
        double bonusScore = (double) bonusResult.get("total");

        // 8. 加权累加总分
        double total = Math.min(100, Math.max(0,
                skillScore + industryScore + experienceScore + educationScore + salaryAdj + locationAdj + bonusScore));

        // 9. 硬性门槛
        boolean hardFilterBlocked = false;
        List<String> hardFilterReasons = new ArrayList<>();

        if (crossIndustry && coverage < 30) {
            hardFilterBlocked = true;
            hardFilterReasons.add("行业方向不匹配且技能覆盖率不足30%");
        }
        if (coverage == 0 && industryFactor < 1.0) {
            Set<String> jobSkills = extractJobSkills(job);
            if (!jobSkills.isEmpty()) {
                hardFilterBlocked = true;
                hardFilterReasons.add("职位要求的技能完全不匹配");
            }
        }
        int rEdu = getEduLevel(resume.getEducation());
        int jEdu = getEduLevel(job.getEducationRequired());
        if (rEdu > 0 && jEdu > 0 && (jEdu - rEdu) >= 2) {
            hardFilterBlocked = true;
            hardFilterReasons.add("学历差距超过2级");
        }

        if (hardFilterBlocked) {
            total = Math.min(total, HARD_FILTER_CAP);
        }

        // 技能零匹配惩罚：技能覆盖率为0%时，总分不超过40
        if (coverage == 0 && total > ZERO_SKILL_CAP) {
            total = ZERO_SKILL_CAP;
            hardFilterReasons.add("技能完全不匹配，仅保留行业/经验/学历基础分");
        }

        // 构建明细
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("total", round(total));
        detail.put("skill", round(skillScore));
        detail.put("skillDetail", skillResult);
        detail.put("skillNormalized", round(skillScore / MAX_SKILL * 100));
        detail.put("industryScore", round(industryScore));
        detail.put("industryFactor", industryFactor);
        detail.put("experienceScore", round(experienceScore));
        detail.put("experienceFactor", round(experienceFactor));
        detail.put("educationScore", round(educationScore));
        detail.put("educationFactor", round(educationFactor));
        detail.put("salaryAdjustment", round(salaryAdj));
        detail.put("locationAdjustment", round(locationAdj));
        detail.put("bonusScore", round(bonusScore));
        detail.put("hardFilterBlocked", hardFilterBlocked);
        detail.put("hardFilterReasons", hardFilterReasons);
        detail.put("bonus", bonusResult);
        detail.put("comparison", buildComparison(resume, job, skillResult));
        detail.put("suggestions", generateOptimizationSuggestions(resume, job, detail));
        return detail;
    }

    // ==================== 技能匹配 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> calcSkillScoreDetail(Resume resume, Job job) {
        Set<String> resumeSkills = parseResumeSkills(resume);
        Set<String> jobSkills = extractJobSkills(job);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumeSkills", new ArrayList<>(resumeSkills));
        result.put("jobSkills", new ArrayList<>(jobSkills));
        result.put("maxScore", MAX_SKILL);

        if (jobSkills.isEmpty()) {
            // 职位无明确技能要求，给30%基础分
            result.put("score", MAX_SKILL * 0.30);
            result.put("exactMatched", Collections.emptyList());
            result.put("fuzzyMatched", Collections.emptyList());
            result.put("missing", Collections.emptyList());
            result.put("extra", new ArrayList<>(resumeSkills));
            result.put("coverage", 0);
            result.put("exactScore", round(MAX_SKILL * 0.30));
            result.put("fuzzyScore", 0.0);
            result.put("depthScore", 0.0);
            return result;
        }
        if (resumeSkills.isEmpty()) {
            result.put("score", 0.0);
            result.put("exactMatched", Collections.emptyList());
            result.put("fuzzyMatched", Collections.emptyList());
            result.put("missing", new ArrayList<>(jobSkills));
            result.put("extra", Collections.emptyList());
            result.put("coverage", 0);
            result.put("exactScore", 0.0);
            result.put("fuzzyScore", 0.0);
            result.put("depthScore", 0.0);
            return result;
        }

        List<String> exactMatched = new ArrayList<>();
        List<String> fuzzyMatched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> extra = new ArrayList<>(resumeSkills);

        for (String js : jobSkills) {
            String jsNorm = normalize(js);
            boolean found = false;
            for (String rs : resumeSkills) {
                if (normalize(rs).equals(jsNorm)) {
                    exactMatched.add(js); extra.remove(rs); found = true; break;
                }
            }
            if (!found) {
                for (String rs : resumeSkills) {
                    if (isAlias(rs, js)) {
                        fuzzyMatched.add(js); extra.remove(rs); found = true; break;
                    }
                }
            }
            if (!found) missing.add(js);
        }

        // v10优化：引入技能重要性加权 + 调整覆盖率奖励
        // 1. 计算技能重要性权重
        Map<String, Double> skillWeights = extractSkillWeights(job);
        
        // 2. 计算加权匹配分数
        double totalWeight = skillWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        double matchedWeight = 0;
        double fuzzyWeight = 0;
        
        for (Map.Entry<String, Double> entry : skillWeights.entrySet()) {
            String jobSkill = entry.getKey();
            double weight = entry.getValue();
            
            if (exactMatched.contains(jobSkill)) {
                matchedWeight += weight;  // 精确匹配获得全额权重
            } else if (fuzzyMatched.contains(jobSkill)) {
                fuzzyWeight += weight * 0.7;  // 别名匹配获得70%权重
            }
        }
        
        // 3. 基础分数计算（加权）
        double weightedExactScore = (totalWeight > 0) ? (matchedWeight / totalWeight) * MAX_SKILL * 0.70 : 0;
        double weightedFuzzyScore = (totalWeight > 0) ? (fuzzyWeight / totalWeight) * MAX_SKILL * 0.25 : 0;
        
        // 4. 深度加分：额外相关技能
        double depthScore = 0;
        if (!extra.isEmpty()) {
            long relevant = extra.stream().filter(es -> {
                String esNorm = normalize(es);
                return jobSkills.stream().anyMatch(js -> {
                    Set<String> aliases = getAliasGroup(normalize(js));
                    return aliases != null && aliases.contains(esNorm);
                });
            }).count();
            depthScore = Math.min(MAX_SKILL * 0.08, (double) relevant / jobSkills.size() * MAX_SKILL * 0.08);
        }

        // 5. 覆盖率奖励优化：降低过高奖励，防止刷分
        int cov = (int) Math.round((double) (exactMatched.size() + fuzzyMatched.size()) / jobSkills.size() * 100);
        double coverageBonus = 0;
        if (cov >= 100) coverageBonus = MAX_SKILL * 0.08;   // 从12%降至8%
        else if (cov >= 90) coverageBonus = MAX_SKILL * 0.05; // 从8%降至5%
        else if (cov >= 70) coverageBonus = MAX_SKILL * 0.02; // 从3%降至2%

        double total = weightedExactScore + weightedFuzzyScore + depthScore + coverageBonus;
        result.put("score", Math.min(MAX_SKILL, total));
        result.put("exactMatched", exactMatched);
        result.put("fuzzyMatched", fuzzyMatched);
        result.put("missing", missing);
        result.put("extra", extra);
        result.put("coverage", cov);
        result.put("exactScore", round(weightedExactScore));
        result.put("fuzzyScore", round(weightedFuzzyScore));
        result.put("depthScore", round(depthScore));
        result.put("skillWeights", skillWeights); // 新增：返回权重信息供前端展示
        return result;
    }
    
    /**
     * 提取技能重要性权重 - v10新增
     * 根据职位描述中的关键词判断技能重要性
     */
    private Map<String, Double> extractSkillWeights(Job job) {
        // v10优化：使用缓存避免重复计算
        String cacheKey = job.getId() + "_" + 
            (job.getRequirements() != null ? job.getRequirements().hashCode() : 0) + "_" +
            (job.getDescription() != null ? job.getDescription().hashCode() : 0);
        
        synchronized (skillWeightsCache) {
            if (skillWeightsCache.containsKey(cacheKey)) {
                return skillWeightsCache.get(cacheKey);
            }
        }
        
        Map<String, Double> weights = new HashMap<>();
        String requirements = safe(job.getRequirements()).toLowerCase();
        String description = safe(job.getDescription()).toLowerCase();
        String title = safe(job.getTitle()).toLowerCase();
        
        Set<String> jobSkills = extractJobSkills(job);
        
        for (String skill : jobSkills) {
            String skillLower = skill.toLowerCase();
            double weight = 0.5; // 默认中等权重
            
            // 高权重标识：必须、精通、熟练掌握、核心
            if (requirements.contains("必须" + skillLower) || 
                requirements.contains("精通" + skillLower) ||
                requirements.contains("熟练掌握" + skillLower) ||
                requirements.contains(skillLower + "必备") ||
                title.contains(skillLower)) {
                weight = 1.0;
            }
            // 中高权重：熟悉、主导、负责
            else if (requirements.contains("熟悉" + skillLower) ||
                     requirements.contains("主导" + skillLower) ||
                     description.contains("精通" + skillLower)) {
                weight = 0.8;
            }
            // 中权重：了解、优先、加分项
            else if (requirements.contains("了解" + skillLower) ||
                     requirements.contains(skillLower + "优先") ||
                     description.contains("熟悉" + skillLower)) {
                weight = 0.6;
            }
            // 低权重：仅提及
            else if (description.contains(skillLower)) {
                weight = 0.4;
            }
            
            weights.put(skill, weight);
        }
        
        // v10优化：缓存结果（LRU策略）
        synchronized (skillWeightsCache) {
            if (skillWeightsCache.size() >= SKILL_WEIGHTS_CACHE_MAX_SIZE) {
                // 简单LRU：删除第一个元素
                String firstKey = skillWeightsCache.keySet().iterator().next();
                skillWeightsCache.remove(firstKey);
            }
            skillWeightsCache.put(cacheKey, weights);
        }
        
        return weights;
    }

    // ==================== 行业系数 ====================

    private double calcIndustryFactor(Resume resume, Job job) {
        Long rCat = resume.getCategoryId();
        if (rCat == null) rCat = inferCategoryId(resume);
        Long jCat = job.getCategoryId();
        if (rCat != null && jCat != null) {
            if (rCat.equals(jCat)) return 1.0;
            Set<Long> related = RELATED_CATEGORIES.get(rCat);
            if (related != null && related.contains(jCat)) return 0.6;
            return 0.2;
        }
        Set<String> rInd = inferIndustry(resume.getSelfIntroduction() + " " + resume.getWorkExperience());
        Set<String> jInd = inferIndustry(job.getTitle() + " " + job.getRequirements() + " " + job.getDescription());
        if (!rInd.isEmpty() && !jInd.isEmpty()) {
            Set<String> inter = new HashSet<>(rInd); inter.retainAll(jInd);
            return inter.isEmpty() ? 0.2 : 0.7;
        }
        return 0.5;
    }

    private Set<String> inferIndustry(String text) {
        Set<String> result = new HashSet<>();
        if (text == null) return result;
        String lower = text.toLowerCase();
        for (Map.Entry<String, Set<String>> e : INDUSTRY_KEYWORDS.entrySet()) {
            for (String kw : e.getValue()) {
                if (lower.contains(kw.toLowerCase())) { result.add(e.getKey()); break; }
            }
        }
        return result;
    }

    /**
     * 从简历内容推断行业分类ID。
     * 当简历没有设置 categoryId 时，通过分析工作经历、自我介绍、技能等文本
     * 推断最可能的行业，用于预筛选职位。
     */
    private Long inferCategoryId(Resume resume) {
        String text = safe(resume.getWorkExperience()) + " " + safe(resume.getSelfIntroduction())
                + " " + safe(resume.getSkills()) + " " + safe(resume.getName());
        Set<String> industries = inferIndustry(text);
        if (industries.isEmpty()) return null;

        // 统计各行业命中关键词数，选最多的
        String best = null;
        int bestCount = 0;
        String lower = text.toLowerCase();
        for (String ind : industries) {
            Set<String> kws = INDUSTRY_KEYWORDS.get(ind);
            if (kws == null) continue;
            int count = 0;
            for (String kw : kws) {
                if (lower.contains(kw.toLowerCase())) count++;
            }
            if (count > bestCount) { bestCount = count; best = ind; }
        }

        // 行业名 → categoryId 映射
        if (best == null) return null;
        return switch (best) {
            case "IT" -> 1L;
            case "FINANCE" -> 2L;
            case "EDUCATION" -> 3L;
            case "MEDICAL" -> 4L;
            case "MANUFACTURING" -> 5L;
            case "SALES" -> 6L;
            case "ADMIN" -> 7L;
            case "CONSTRUCTION" -> 8L;
            case "MEDIA" -> 9L;
            case "SERVICE" -> 10L;
            default -> null;
        };
    }

    // ==================== 经验系数 ====================

    private double calcExperienceFactor(Resume resume, Job job) {
        int rYears = extractYears(resume.getExperience());
        int jMin = extractMinYears(job.getExperienceRequired());
        int jMax = extractMaxYears(job.getExperienceRequired());
        
        // v10优化：计算基础经验系数
        double baseFactor;
        if (rYears == 0 && jMin == 0) baseFactor = 0.7;
        else if (rYears == 0) baseFactor = 0.4;
        else if (jMin > 0) {
            double ratio = (double) rYears / jMin;
            if (jMax > 0 && rYears > jMax * 1.5) ratio = Math.min(ratio, 1.3);
            if (ratio >= 1.0) baseFactor = 1.0;
            else if (ratio >= 0.8) baseFactor = 0.85;
            else if (ratio >= 0.5) baseFactor = 0.6;
            else if (ratio >= 0.3) baseFactor = 0.4;
            else baseFactor = 0.3;
        } else {
            // 无经验要求的职位，有经验的候选人应获得较高分数
            if (rYears >= 8) baseFactor = 1.0;
            else if (rYears >= 5) baseFactor = 0.95;
            else if (rYears >= 3) baseFactor = 0.9;
            else if (rYears >= 1) baseFactor = 0.85;
            else baseFactor = 0.7;
        }
        
        // v10新增：应用经验质量调整因子
        double qualityFactor = calculateExperienceQualityFactor(resume, job);
        
        // 综合系数 = 基础系数 * 质量因子（范围0.8-1.2）
        return baseFactor * qualityFactor;
    }
    
    /**
     * v10新增：计算经验质量调整因子
     * 基于工作经历详细度、关键词匹配、项目复杂度等维度
     */
    private double calculateExperienceQualityFactor(Resume resume, Job job) {
        double factor = 1.0;
        
        // 1. 工作经历详细度检查
        String workExp = safe(resume.getWorkExperience());
        if (workExp.length() > 300) {
            factor += 0.1;  // 非常详细 +10%
        } else if (workExp.length() > 150) {
            factor += 0.05; // 较详细 +5%
        } else if (workExp.length() < 50) {
            factor -= 0.1;  // 过于简单 -10%
        }
        
        // 2. 项目成果关键词检测
        String lowerExp = workExp.toLowerCase();
        String[] achievementKeywords = {
            "负责", "主导", "独立完成", "从0到1", "架构设计", "性能优化",
            "提升", "改进", "重构", "解决", "攻克", "实现",
            // 医疗行业
            "手术", "诊断", "治愈", "抢救", "临床", "发表论文", "SCI",
            "课题", "科研", "规培", "带教", "查房", "会诊", "病历",
            // 教育行业
            "教学成果", "教研", "获奖", "竞赛", "升学率", "优秀教师",
            // 金融行业
            "审计", "风控", "合规", "管理资产", "收益率", "项目融资",
            // 通用
            "业绩", "超额完成", "客户满意度", "团队管理", "培训"
        };
        int keywordCount = 0;
        for (String kw : achievementKeywords) {
            if (lowerExp.contains(kw)) keywordCount++;
        }
        
        if (keywordCount >= 5) {
            factor += 0.1;  // 丰富的项目成果描述 +10%
        } else if (keywordCount >= 3) {
            factor += 0.05; // 较好的项目成果描述 +5%
        } else if (keywordCount == 0 && workExp.length() > 100) {
            factor -= 0.05; // 缺少成果动词 -5%
        }
        
        // 3. 技术栈深度检测（是否提到具体技术应用场景）
        String[] depthIndicators = {
            "高并发", "分布式", "微服务", "集群", "负载均衡",
            "源码", "原理", "底层", "调优", "最佳实践",
            // 医疗行业深度指标
            "三甲", "二甲", "主治", "副主任", "主任", "学科带头人",
            "新技术", "微创", "介入", "疑难", "危重", "ICU",
            // 教育行业
            "精品课程", "教学改革", "课程思政", "双一流",
            // 金融行业
            "风险评估", "资产配置", "量化", "投研"
        };
        int depthCount = 0;
        for (String indicator : depthIndicators) {
            if (lowerExp.contains(indicator)) depthCount++;
        }
        
        if (depthCount >= 3) {
            factor += 0.1;  // 深度技术经验 +10%
        } else if (depthCount >= 1) {
            factor += 0.05; // 一定技术深度 +5%
        }
        
        // 4. 工作年限与经历详细度的合理性检查
        int years = extractYears(resume.getExperience());
        if (years > 0 && workExp.length() > 0) {
            double charsPerYear = (double) workExp.length() / years;
            if (charsPerYear < 20) {
                factor -= 0.1;  // 多年经验但描述过简，可疑 -10%
            }
        }
        
        // 限制因子范围在0.8-1.2之间
        return Math.max(0.8, Math.min(1.2, factor));
    }

    // ==================== 学历系数 ====================

    private double calcEducationFactor(Resume resume, Job job) {
        int r = getEduLevel(resume.getEducation());
        int j = getEduLevel(job.getEducationRequired());
        if (j == 0) return 0.7;
        if (r >= j) return 1.0;
        if (r == j - 1) return 0.6;
        if (r == j - 2) return 0.3;
        return 0.2;
    }

    // ==================== 薪资微调 ====================

    private double calcSalaryAdjustment(Resume resume, Job job) {
        if (resume.getExpectedSalary() == null || job.getSalaryMin() == null || job.getSalaryMax() == null) return 0;
        double exp = resume.getExpectedSalary().doubleValue();
        double min = job.getSalaryMin().doubleValue(), max = job.getSalaryMax().doubleValue();
        if (exp >= min && exp <= max) return 2.0;
        if (exp < min) {
            double gap = (min - exp) / min;
            if (gap <= 0.1) return 1.0;
            if (gap <= 0.3) return 0;
            return -2.0;
        }
        double gap = (exp - max) / max;
        if (gap <= 0.1) return 0;
        if (gap <= 0.3) return -1.0;
        return -3.0;
    }

    // ==================== 地点微调 ====================

    private double calcLocationAdjustment(Resume resume, Job job) {
        String jLoc = extractCity(job.getLocation());
        if (jLoc == null) return 0;
        String rText = safe(resume.getSelfIntroduction()) + " " + safe(resume.getWorkExperience());
        String rLoc = extractCity(rText);
        if (rLoc == null) return 0;
        if (rLoc.equals(jLoc)) return 1.0;
        Set<String> t1 = Set.of("北京", "上海", "广州", "深圳");
        if (t1.contains(rLoc) && t1.contains(jLoc)) return 0;
        return -1.0;
    }

    // ==================== 加分 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> calcBonusPoints(Resume resume, Job job, Map<String, Object> skillResult, double eduFactor) {
        Map<String, Object> bonus = new LinkedHashMap<>();
        double total = 0;
        List<String> missing = (List<String>) skillResult.get("missing");
        List<String> jobSkills = (List<String>) skillResult.get("jobSkills");
        
        // 1. 全技能匹配加分（保持不变）
        if (jobSkills != null && !jobSkills.isEmpty() && (missing == null || missing.isEmpty())) {
            bonus.put("allSkillsMatched", 3.0); total += 3;
        }
        
        // 2. v10优化：工作经历详细度加分（更细化）
        String workExp = safe(resume.getWorkExperience());
        if (workExp.length() > 300) {
            bonus.put("detailedExperience", 3.0); total += 3;  // 从2分提升到3分
        } else if (workExp.length() > 150) {
            bonus.put("moderateExperience", 2.0); total += 2;
        } else if (workExp.length() > 80) {
            bonus.put("basicExperience", 1.0); total += 1;
        }
        
        // 3. v10新增：项目成果关键词加分
        String lowerExp = workExp.toLowerCase();
        String[] highValueKeywords = {"主导", "架构设计", "从0到1", "性能优化", "重构"};
        int highValueCount = 0;
        for (String kw : highValueKeywords) {
            if (lowerExp.contains(kw)) highValueCount++;
        }
        if (highValueCount >= 3) {
            bonus.put("achievementKeywords", 2.0); total += 2;  // 高质量项目经验
        } else if (highValueCount >= 1) {
            bonus.put("someAchievements", 1.0); total += 1;
        }
        
        // 4. 学历超标加分（保持不变）
        int rEdu = getEduLevel(resume.getEducation());
        int jEdu = getEduLevel(job.getEducationRequired());
        if (jEdu > 0 && rEdu > jEdu) {
            double b = rEdu - jEdu >= 2 ? 3.0 : 2.0;
            bonus.put("educationExceed", b); total += b;
        }
        
        // 5. v10新增：技术深度指标加分
        String[] depthIndicators = {"高并发", "分布式", "微服务", "源码", "原理", "调优"};
        int depthCount = 0;
        for (String indicator : depthIndicators) {
            if (lowerExp.contains(indicator)) depthCount++;
        }
        if (depthCount >= 3) {
            bonus.put("technicalDepth", 2.0); total += 2;  // 深度技术能力
        } else if (depthCount >= 1) {
            bonus.put("someDepth", 1.0); total += 1;
        }
        
        // 6. 自我介绍与职位匹配加分（保持不变）
        String selfIntro = safe(resume.getSelfIntroduction()).toLowerCase();
        String jobReq = (safe(job.getRequirements()) + " " + safe(job.getDescription())).toLowerCase();
        if (!selfIntro.isEmpty() && !jobReq.isEmpty()) {
            for (String kw : new String[]{"负责","主导","精通","擅长","熟悉","架构","优化"}) {
                if (selfIntro.contains(kw) && jobReq.contains(kw)) {
                    bonus.put("selfIntroMatch", 1.0); total += 1; break;
                }
            }
        }
        
        // 7. v10新增：简历完整度加分
        int completenessScore = calculateResumeCompleteness(resume);
        if (completenessScore >= 90) {
            bonus.put("highCompleteness", 1.0); total += 1;  // 简历非常完整
        }
        
        // 8. v10新增：快速响应加分（根据创建时间）
        if (resume.getCreateTime() != null) {
            long hoursSinceCreation = java.time.temporal.ChronoUnit.HOURS.between(
                resume.getCreateTime(), LocalDateTime.now()
            );
            if (hoursSinceCreation <= 24) {
                bonus.put("quickResponse", 1.0); total += 1;  // 24小时内完善简历
            }
        }
        
        bonus.put("total", Math.min(MAX_BONUS, total));
        bonus.put("maxBonus", MAX_BONUS);
        return bonus;
    }
    
    /**
     * v10新增：计算简历完整度分数（0-100）
     */
    private int calculateResumeCompleteness(Resume resume) {
        int score = 0;
        int maxScore = 100;
        
        // 基本信息（30分）
        if (resume.getName() != null && !resume.getName().isEmpty()) score += 10;
        if (resume.getAge() != null) score += 5;
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) score += 10;
        if (resume.getExpectedSalary() != null) score += 5;
        
        // 技能（20分）
        if (resume.getSkills() != null && !resume.getSkills().isEmpty() && !"[]".equals(resume.getSkills())) {
            score += 20;
        }
        
        // 经验（25分）
        if (resume.getExperience() != null && !resume.getExperience().isEmpty()) score += 10;
        if (resume.getWorkExperience() != null && resume.getWorkExperience().length() > 50) score += 15;
        
        // 自我评价（15分）
        if (resume.getSelfIntroduction() != null && resume.getSelfIntroduction().length() > 30) {
            score += 15;
        }
        
        // 分类（10分）
        if (resume.getCategoryId() != null) score += 10;
        
        return Math.min(maxScore, score);
    }

    // ==================== 对照信息 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildComparison(Resume resume, Job job, Map<String, Object> skillResult) {
        Map<String, Object> comp = new LinkedHashMap<>();
        Map<String, Object> edu = new LinkedHashMap<>();
        edu.put("requirement", nvl(job.getEducationRequired(), "不限"));
        edu.put("actual", nvl(resume.getEducation(), "未填写"));
        edu.put("matched", getEduLevel(resume.getEducation()) >= getEduLevel(job.getEducationRequired()));
        comp.put("education", edu);
        Map<String, Object> exp = new LinkedHashMap<>();
        exp.put("requirement", nvl(job.getExperienceRequired(), "不限"));
        exp.put("actual", nvl(resume.getExperience(), "未填写"));
        int rY = extractYears(resume.getExperience()), jY = extractMinYears(job.getExperienceRequired());
        exp.put("matched", jY == 0 || rY >= jY);
        exp.put("resumeYears", rY);
        exp.put("requiredYears", jY);
        comp.put("experience", exp);
        Map<String, Object> sal = new LinkedHashMap<>();
        sal.put("range", formatSalaryRange(job.getSalaryMin(), job.getSalaryMax()));
        sal.put("expected", resume.getExpectedSalary() != null ? "¥" + resume.getExpectedSalary().toPlainString() : "未填写");
        boolean sm = true;
        if (resume.getExpectedSalary() != null && job.getSalaryMax() != null)
            sm = resume.getExpectedSalary().compareTo(job.getSalaryMax()) <= 0;
        sal.put("matched", sm);
        comp.put("salary", sal);
        Map<String, Object> sk = new LinkedHashMap<>();
        sk.put("matched", skillResult.get("exactMatched"));
        sk.put("fuzzyMatched", skillResult.get("fuzzyMatched"));
        sk.put("missing", skillResult.get("missing"));
        sk.put("extra", skillResult.get("extra"));
        sk.put("coverage", skillResult.get("coverage"));
        comp.put("skill", sk);
        Map<String, Object> ind = new LinkedHashMap<>();
        Long rCat = resume.getCategoryId();
        if (rCat == null) rCat = inferCategoryId(resume);
        ind.put("resumeCategory", rCat);
        ind.put("jobCategory", job.getCategoryId());
        boolean same = rCat != null && rCat.equals(job.getCategoryId());
        boolean related = rCat != null && isRelatedCategory(rCat, job.getCategoryId());
        ind.put("sameIndustry", same);
        ind.put("relatedIndustry", related);
        ind.put("crossIndustry", !same && !related);
        comp.put("industry", ind);
        return comp;
    }

    // ==================== 优化建议 ====================

    @SuppressWarnings("unchecked")
    private List<String> generateOptimizationSuggestions(Resume resume, Job job, Map<String, Object> detail) {
        List<String> suggestions = new ArrayList<>();
        
        // 1. 技能缺失建议（v10优化：按重要性排序）
        Map<String, Object> sd = (Map<String, Object>) detail.get("skillDetail");
        if (sd != null) {
            List<String> missing = (List<String>) sd.get("missing");
            Map<String, Double> skillWeights = (Map<String, Double>) sd.get("skillWeights");
            
            if (missing != null && !missing.isEmpty()) {
                // 按权重排序，优先补充重要技能
                List<String> sortedMissing = new ArrayList<>(missing);
                if (skillWeights != null) {
                    sortedMissing.sort((a, b) -> {
                        double weightA = skillWeights.getOrDefault(a, 0.5);
                        double weightB = skillWeights.getOrDefault(b, 0.5);
                        return Double.compare(weightB, weightA); // 降序
                    });
                }
                
                List<String> topMissing = sortedMissing.subList(0, Math.min(5, sortedMissing.size()));
                StringBuilder sb = new StringBuilder("🎯 建议优先补充核心技能：");
                for (int i = 0; i < topMissing.size(); i++) {
                    String skill = topMissing.get(i);
                    Double weight = skillWeights != null ? skillWeights.get(skill) : null;
                    sb.append(skill);
                    if (weight != null && weight >= 0.8) {
                        sb.append("(重要)");
                    }
                    if (i < topMissing.size() - 1) sb.append("、");
                }
                suggestions.add(sb.toString());
            }
            
            // 覆盖率提示
            Integer cov = (Integer) sd.get("coverage");
            if (cov != null && cov < 50) {
                suggestions.add("⚠️ 技能覆盖率仅" + cov + "%，建议补充更多相关技能");
            }
        }
        
        // 2. v10新增：经验质量建议
        String workExp = safe(resume.getWorkExperience());
        if (workExp.length() < 100 && extractYears(resume.getExperience()) > 2) {
            suggestions.add("💼 工作经历描述过简，建议补充项目成果和具体职责");
        }
        
        String[] achievementKeywords = {"负责", "主导", "独立完成", "架构设计", "性能优化"};
        boolean hasAchievement = false;
        for (String kw : achievementKeywords) {
            if (workExp.contains(kw)) {
                hasAchievement = true;
                break;
            }
        }
        if (!hasAchievement && workExp.length() > 50) {
            suggestions.add("✨ 建议突出项目成果，使用'主导'、'优化'、'提升'等动词");
        }
        
        // 3. 学历建议（保持不变）
        int rEdu = getEduLevel(resume.getEducation()), jEdu = getEduLevel(job.getEducationRequired());
        if (jEdu > 0 && rEdu > 0 && rEdu < jEdu) {
            if (jEdu - rEdu >= 2) {
                suggestions.add("🎓 学历差距较大，建议通过证书或项目经验弥补");
            } else {
                suggestions.add("🎓 该职位偏好" + job.getEducationRequired() + "学历");
            }
        }
        
        // 4. v10优化：经验年限建议
        int rY = extractYears(resume.getExperience()), jY = extractMinYears(job.getExperienceRequired());
        if (jY > 0 && rY > 0) {
            if (rY < jY) {
                int gap = jY - rY;
                if (gap >= 3) {
                    suggestions.add("⏰ 经验差距" + gap + "年，建议强调学习能力和成长潜力");
                } else if (gap >= 2) {
                    suggestions.add("⏰ 经验略有不足，建议突出项目复杂度与技术深度");
                }
            } else if (rY > jY * 2) {
                suggestions.add("💡 经验丰富，建议展示管理能力和架构思维");
            }
        }
        
        // 5. 行业建议（保持不变）
        Map<String, Object> comp = (Map<String, Object>) detail.get("comparison");
        if (comp != null) {
            Map<String, Object> ic = (Map<String, Object>) comp.get("industry");
            if (ic != null && Boolean.TRUE.equals(ic.get("crossIndustry"))) {
                suggestions.add("🔄 跨行业求职，建议在自我介绍中强调可迁移技能和转行动机");
            }
        }
        
        // 6. v10新增：薪资建议
        if (resume.getExpectedSalary() != null && job.getSalaryMin() != null && job.getSalaryMax() != null) {
            double exp = resume.getExpectedSalary().doubleValue();
            double max = job.getSalaryMax().doubleValue();
            if (exp > max * 1.3) {
                suggestions.add("💰 期望薪资超出职位预算较多，建议调整或说明理由");
            } else if (exp < job.getSalaryMin().doubleValue() * 0.7) {
                suggestions.add("💰 期望薪资偏低，可能影响HR对您能力的判断");
            }
        }
        
        // 7. v10新增：简历完整度建议
        int completeness = calculateResumeCompleteness(resume);
        if (completeness < 70) {
            suggestions.add("📝 简历完整度" + completeness + "%，建议补充更多信息提高竞争力");
        }
        
        // 8. 如果没有问题，给出积极建议
        if (suggestions.isEmpty()) {
            double totalScore = Double.parseDouble(detail.get("total").toString());
            if (totalScore >= 75) {
                suggestions.add("✅ 匹配度优秀，建议立即投递！");
            } else if (totalScore >= 60) {
                suggestions.add("👍 匹配度良好，建议完善细节后投递");
            } else {
                suggestions.add("💪 有一定匹配度，建议针对性优化后再投递");
            }
        }
        
        return suggestions;
    }

    // ==================== 建议文案 ====================

    @SuppressWarnings("unchecked")
    private String generateSuggestion(Resume resume, Job job, BigDecimal score, Map<String, Object> detail) {
        int sc = score.intValue();
        Map<String, Object> sd = (Map<String, Object>) detail.get("skillDetail");
        StringBuilder sb = new StringBuilder();
        if (sc >= 75) sb.append("高度匹配");
        else if (sc >= 55) sb.append("匹配度良好");
        else if (sc >= 35) sb.append("匹配度一般");
        else if (sc >= 20) sb.append("匹配度较低");
        else sb.append("不推荐");
        if (sd != null) {
            Integer cov = (Integer) sd.get("coverage");
            if (cov != null && cov > 0) sb.append("，技能覆盖率").append(cov).append("%");
            List<String> matched = (List<String>) sd.get("exactMatched");
            if (matched != null && !matched.isEmpty())
                sb.append("：").append(String.join("、", matched.subList(0, Math.min(3, matched.size()))));
        }
        if (Boolean.TRUE.equals(detail.get("hardFilterBlocked"))) sb.append("【不推荐】");
        return sb.toString();
    }

    // ==================== 工具方法 ====================

    private Set<String> parseSkills(String json) {
        Set<String> r = new HashSet<>();
        if (json == null || json.isEmpty() || "[]".equals(json)) return r;
        try { r.addAll(objectMapper.readValue(json, new TypeReference<>() {})); }
        catch (Exception e) {
            for (String s : json.replaceAll("[\\[\\]\"{}]", "").split("[,，、;；]+"))
                if (!s.trim().isEmpty()) r.add(s.trim());
        }
        return r;
    }

    /**
     * 从简历中提取完整技能集合：skills JSON + 工作经历 + 自我介绍
     */
    private Set<String> parseResumeSkills(Resume resume) {
        Set<String> skills = parseSkills(resume.getSkills());
        // 从工作经历和自我介绍中补充提取技能
        String extraText = safe(resume.getWorkExperience()) + " " + safe(resume.getSelfIntroduction());
        if (!extraText.isEmpty()) {
            Set<String> extracted = extractSkillsFromText(extraText.toLowerCase());
            skills.addAll(extracted);
        }
        return skills;
    }

    /**
     * 获取缓存的职位关键词列表（避免 N+1 查询）
     */
    private List<JobKeyword> getCachedJobKeywords() {
        long now = System.currentTimeMillis();
        if (cachedJobKeywords == null || (now - keywordsCacheTime) > KEYWORDS_CACHE_TTL) {
            synchronized (this) {
                if (cachedJobKeywords == null || (now - keywordsCacheTime) > KEYWORDS_CACHE_TTL) {
                    cachedJobKeywords = jobKeywordService.list();
                    keywordsCacheTime = now;
                }
            }
        }
        return cachedJobKeywords;
    }

    /**
     * 提取职位技能要求。
     * 策略：先从 job_keyword 数据库匹配，如果结果为空，则直接从职位描述文本中
     * 用硬编码的技能词库兜底提取，确保不会因为关键词表数据缺失导致技能匹配失效。
     */
    private Set<String> extractJobSkills(Job job) {
        String text = safe(job.getTitle()) + " " + safe(job.getRequirements()) + " " + safe(job.getDescription());
        String lower = text.toLowerCase();

        // 1. 从 job_keyword 数据库匹配（使用缓存避免 N+1 查询）
        Set<String> skills = new HashSet<>();
        for (JobKeyword kw : getCachedJobKeywords()) {
            String k = kw.getKeyword().toLowerCase();
            if (k.matches(".*[\\u4e00-\\u9fa5].*")) {
                if (lower.contains(k)) skills.add(kw.getKeyword());
            } else {
                String regex = "(?<!\\w)" + Pattern.quote(k) + "(?!\\w)";
                if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(lower).find())
                    skills.add(kw.getKeyword());
            }
        }

        // 2. 用硬编码词库补充提取（合并结果，非兜底）
        Set<String> hardcoded = extractSkillsFromText(lower);
        if (!hardcoded.isEmpty()) {
            skills.addAll(hardcoded);
        }

        return skills;
    }

    /**
     * 硬编码技能词库 —— 直接从职位描述文本中提取技能关键词。
     * 作为 job_keyword 数据库的兜底，确保技能匹配不会因为数据缺失而失效。
     */
    private static final List<String> SKILL_KEYWORDS = Arrays.asList(
        // 编程语言
        "java", "python", "go", "golang", "c++", "c#", "c语言", "javascript", "typescript",
        "php", "ruby", "scala", "rust", "swift", "kotlin", "dart", "perl", "lua", "r语言",
        "objective-c", "objc", "汇编", "assembly",
        // 前端框架/库
        "vue", "vue.js", "vue3", "vue2", "react", "react.js", "reactnative", "react native",
        "angular", "angularjs", "jquery", "bootstrap", "element ui", "element-plus", "elementui",
        "ant design", "antd", "vite", "next.js", "nextjs", "nuxt.js", "nuxtjs",
        "svelte", "tailwind", "tailwindcss", "less", "sass", "scss", "stylus",
        "echarts", "d3.js", "three.js", "canvas", "svg", "webgl",
        "微信小程序", "支付宝小程序", "uniapp", "uni-app", "taro", "mpvue",
        // 后端框架
        "spring", "spring boot", "springboot", "spring cloud", "springcloud", "spring mvc",
        "springmvc", "spring security", "springsecurity", "spring data",
        "mybatis", "mybatis-plus", "mybatisplus", "hibernate", "jpa",
        "django", "flask", "fastapi", "tornado", "bottle",
        ".net", "asp.net", "aspdotnet", "dotnet core", ".net core",
        "express", "koa", "egg.js", "nestjs", "laravel", "symfony", "codeigniter",
        "gin", "echo", "beego", "fiber", "iris",
        "quarkus", "micronaut", "vert.x", "play framework",
        // 数据库
        "mysql", "postgresql", "postgres", "oracle", "sql server", "sqlserver", "mariadb",
        "mongodb", "redis", "elasticsearch", "elastic search", "es",
        "cassandra", "hbase", "neo4j", "sqlite", "dynamodb", "cosmosdb",
        "tidb", "oceanbase", "dm", "达梦", "人大金仓", "gaussdb", "opengauss",
        "memcached", "influxdb", "timescaledb", "clickhouse", "doris",
        // 中间件/消息队列
        "kafka", "rabbitmq", "rocketmq", "activemq", "zookeeper", "nacos", "sentinel",
        "dubbo", "grpc", "thrift", "motan", "tars",
        "eureka", "consul", "etcd", "apollo", "config",
        // 开发工具/运维
        "docker", "kubernetes", "k8s", "jenkins", "git", "gitlab", "github", "gitee",
        "ci/cd", "cicd", "devops", "linux", "centos", "ubuntu", "debian",
        "shell", "bash", "nginx", "apache", "tomcat", "jetty", "undertow",
        "aws", "azure", "gcp", "阿里云", "腾讯云", "华为云", "百度云",
        "maven", "gradle", "npm", "yarn", "pnpm", "pip", "conda",
        "ansible", "terraform", "prometheus", "grafana", "zabbix", "elk",
        "sonar", "sonarqube", "jira", "confluence", "禅道", "飞书", "钉钉",
        // 前端基础技术
        "html", "html5", "css", "css3", "es6", "es2015", "es2020",
        "webpack", "rollup", "vite", "parcel", "babel", "postcss",
        "typescript", "tsx", "jsx", "dom", "bom", "ajax", "axios", "fetch",
        "jsonp", "cors", "http", "https", "tcp", "udp", "websocket",
        // 移动端
        "ios", "android", "flutter", "react native", "weex", "rn",
        "xcode", "android studio", "gradle", "cocoapods", "carthage",
        "swiftui", "jetpack compose", "compose", "kotlin multiplatform",
        // AI/大数据
        "ai", "人工智能", "机器学习", "深度学习", "nlp", "自然语言处理",
        "计算机视觉", "cv", "大模型", "llm", "gpt", "chatgpt", "transformer", "bert",
        "hadoop", "spark", "flink", "hive", "数据仓库", "大数据", "数据治理",
        "tensorflow", "pytorch", "keras", "scikit-learn", "sklearn",
        "pandas", "numpy", "scipy", "matplotlib",
        "推荐系统", "知识图谱", "图神经网络", "强化学习",
        "数据挖掘", "数据分析", "数据可视化", "bi", "etl", "airflow",
        "数据湖", "delta lake", "iceberg", "hudi",
        // 架构/概念
        "微服务", "微服务架构", "分布式", "分布式系统", "高并发", "高可用",
        "负载均衡", "缓存", "搜索引擎", "全文检索",
        "restful", "rest", "graphql", "websocket", "grpc", "protobuf",
        "rpc", "soa", "ddd", "领域驱动", "cqrs", "event sourcing",
        "设计模式", "单例", "工厂", "观察者", "策略模式", "代理模式",
        "面向对象", "oop", "函数式", "fp", "响应式", "reactive",
        "异步", "并发", "多线程", "线程池", "协程", "nio", "io多路复用",
        "jvm", "gc", "内存模型", "类加载", "字节码",
        "性能优化", "性能调优", "压测", "压力测试", "jmeter",
        "代码审查", "code review", "重构", "refactor", "tdd", "bdd",
        // 安全
        "网络安全", "信息安全", "渗透测试", "漏洞扫描", "xss", "csrf", "sql注入",
        "oauth", "jwt", "sso", "单点登录", "rbac", "权限管理", "abac",
        "加密", "ssl", "tls", "https", "ca", "国密",
        // 区块链
        "区块链", "blockchain", "solidity", "智能合约", "web3", "defi", "nft",
        // 游戏
        "游戏开发", "unity", "unreal", "ue4", "ue5", "cocos", "cocos2d",
        "游戏引擎", "shader", "渲染", "物理引擎",
        // 测试
        "单元测试", "集成测试", "自动化测试", "selenium", "cypress", "playwright",
        "junit", "pytest", "mocha", "jest", "postman",
        // 产品/设计/运营（非技术岗也能匹配到）
        "产品经理", "产品设计", "ui设计", "ux", "交互设计", "用户研究",
        "原型设计", "figma", "sketch", "adobe", "photoshop", "illustrator",
        "运营", "用户增长", "seo", "sem", "内容运营", "社群运营",
        "项目管理", "pmp", "敏捷", "scrum", "kanban", "看板",
        // 教育/培训
        "教师资格证", "教师证", "教师资格", "教学能力",
        "班主任", "授课", "备课", "教研", "课程设计", "课程开发", "教案",
        "学科带头人", "骨干教师", "特级教师", "高级教师", "一级教师", "二级教师",
        "教育学", "心理学", "教育心理学", "学科教学",
        "幼儿教育", "学前教育", "小学教育", "中学教育", "高等教育", "职业教育",
        "托育", "早教", "启蒙教育", "亲子教育",
        "一对一辅导", "小班教学", "大班教学", "在线教育", "网课", "直播授课",
        // 语言/证书
        "英语四级", "英语六级", "cet4", "cet6", "cet-4", "cet-6",
        "英语专业四级", "英语专业八级", "专四", "专八", "tem4", "tem8",
        "雅思", "ielts", "托福", "toefl", "gre", "gmat", "商务英语", "bec",
        "日语n1", "日语n2", "日语n3", "jlpt", "韩语topik",
        "普通话", "普通话等级", "普通话二甲", "普通话二乙", "普通话一乙", "普通话一甲",
        "翻译资格证", "catti", "口译", "笔译", "同声传译",
        // 职业资格证书
        "注册会计师", "cpa", "acca", "cfa", "frm", "初级会计", "中级会计", "高级会计",
        "律师资格", "司法考试", "法考", "法律职业资格",
        "执业医师", "执业药师", "护士资格", "护师", "主管护师",
        "建造师", "一级建造师", "二级建造师", "造价师", "造价工程师",
        "注册电气工程师", "注册结构工程师", "注册消防工程师",
        "人力资源管理师", "心理咨询师", "营养师", "健康管理师",
        "电工证", "焊工证", "叉车证", "特种作业",
        "驾驶证", "驾照", "c1驾照", "b2驾照", "a1驾照",
        "安全员", "安全工程师", "注册安全工程师",
        "证券从业", "基金从业", "银行从业", "期货从业",
        // 医疗/健康
        "临床", "护理", "药学", "医学影像", "检验", "康复", "中医", "针灸", "推拿",
        "全科医生", "内科", "外科", "妇产科", "儿科", "口腔", "眼科", "耳鼻喉",
        "影像诊断", "b超", "ct", "mri", "x光", "心电图",
        "病历", "病案", "处方", "临床路径", "医疗质量管理",
        "心血管", "骨科", "肿瘤", "神经科", "呼吸科", "消化科", "泌尿科", "皮肤科",
        "急诊", "重症", "icu", "麻醉", "手术室", "门诊", "病房", "住院",
        "执业医师", "执业药师", "护士资格", "护师", "主管护师", "主治医师", "副主任医师", "主任医师",
        "规培", "住院医师", "临床诊断", "药物分析", "药剂", "中药学",
        "放射", "核医学", "病理", "输血", "消毒", "院感", "公共卫生",
        "健康体检", "慢病管理", "家庭医生", "社区卫生", "急救", "120",
        "心理咨询", "心理治疗", "精神科", "睡眠医学",
        // 金融/财务
        "财务分析", "财务报表", "会计核算", "成本核算", "税务", "税务筹划",
        "审计", "内审", "外审", "风控", "风险控制", "合规",
        "投融资", "ipo", "并购", "尽职调查", "估值",
        "基金", "股票", "债券", "理财", "资产管理", "财富管理",
        "信贷", "贷款", "风控模型", "信用评估",
        // 法律
        "合同审查", "合同管理", "法律咨询", "法律顾问", "知识产权", "专利", "商标", "著作权",
        "劳动法", "合同法", "公司法", "知识产权法", "刑法", "民法", "行政法",
        "诉讼", "仲裁", "调解", "法律文书", "尽职调查",
        // 销售/市场
        "客户开发", "客户维护", "客户管理", "渠道销售", "直销",
        "b2b", "b2c", "o2o", "saas", "crm",
        "市场推广", "品牌推广", "品牌策划", "市场调研", "竞品分析",
        "新媒体", "短视频", "抖音", "快手", "小红书", "微信公众号",
        "文案", "文案策划", "文案撰写", "创意策划", "活动策划", "活动执行",
        // 人事/行政
        "面试技巧", "人才测评", "猎头",
        "薪酬", "薪酬福利", "绩效考核", "绩效管理", "kpi", "okr",
        "培训", "培训管理", "员工关系", "劳动关系", "社保", "公积金",
        "行政管理", "行政事务", "办公管理", "会议管理", "档案管理",
        // 工程/制造
        "质量管控", "质量管理", "qa", "qc", "品质管理", "iso", "iso9001", "iso14001",
        "工艺", "工艺流程", "生产管理", "生产计划", "精益生产", "ie", "pe",
        "供应链", "供应链管理", "采购", "采购管理", "供应商管理", "物流", "仓储",
        "机械设计", "cad", "solidworks", "catia", "ug", "pro/e", "autocad",
        "电气", "plc", "单片机", "嵌入式", "arm", "fpga", "dsp",
        "建筑", "建筑设计", "结构设计", "暖通", "给排水", "电气设计", "室内设计",
        "景观设计", "城市规划", "市政", "土木工程", "工程造价", "工程管理",
        // 农业/环境
        "农业", "种植", "养殖", "畜牧", "兽医", "水产", "园艺",
        "环保", "环境工程", "污水处理", "废气处理", "固废处理", "碳排放",
        // 通用专业能力（排除就业类型、个人品质、经验描述等非技能项）
        "沟通能力", "表达能力", "团队协作", "团队合作",
        "执行力", "学习能力", "抗压能力"
    );

    /**
     * 从文本中直接匹配硬编码技能词库
     */
    private Set<String> extractSkillsFromText(String lowerText) {
        Set<String> found = new LinkedHashSet<>();
        for (String skill : SKILL_KEYWORDS) {
            String s = skill.toLowerCase();
            // 跳过纯单数字（避免 "8" 从 "八级" 误匹配）
            if (s.matches("\\d")) continue;
            // 英文词用词边界匹配，避免 java 匹配到 javascript
            if (s.matches("[a-z0-9+#.\\-/\\s]+")) {
                String regex = "(?<!\\w)" + Pattern.quote(s) + "(?!\\w)";
                if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(lowerText).find()) {
                    found.add(skill);
                }
            } else {
                // 中文词直接 contains 匹配
                if (lowerText.contains(s)) {
                    found.add(skill);
                }
            }
        }
        return found;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String l = s.toLowerCase().trim();
        String n = SKILL_NORMALIZE.get(l);
        return n != null ? n : l;
    }

    private boolean isAlias(String a, String b) {
        String na = normalize(a), nb = normalize(b);
        if (na.equals(nb)) return true;
        Set<String> g = getAliasGroup(na);
        return g != null && g.contains(nb);
    }

    private Set<String> getAliasGroup(String n) {
        for (Set<String> g : ALIAS_GROUPS)
            for (String s : g)
                if (normalize(s).equals(n)) return g;
        return null;
    }

    /**
     * v10优化：提取经验年限 - 支持范围值取平均
     */
    private int extractYears(String t) {
        if (t == null || t.isEmpty()) return 0;
        
        // 优先匹配 "N-M年" 或 "N~M年" 模式，取平均值
        Matcher m = Pattern.compile("(\\d+)\\s*[-~到至]\\s*(\\d+)\\s*年").matcher(t);
        if (m.find()) {
            int min = Integer.parseInt(m.group(1));
            int max = Integer.parseInt(m.group(2));
            return (min + max) / 2;  // 取平均值，如"3-5年"返回4
        }
        
        // 其次匹配 "N年以上" 模式
        m = Pattern.compile("(\\d+)\\s*年以上").matcher(t);
        if (m.find()) {
            int years = Integer.parseInt(m.group(1));
            return years + 1;  // "3年以上"按4年算（保守估计）
        }
        
        // 再次匹配 "N年" 模式
        m = Pattern.compile("(\\d+)\\s*年").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        
        // 最后匹配 "工作经验" 后面的数字
        m = Pattern.compile("工作经验[：:]?\\s*(\\d+)").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        
        return 0;
    }

    /**
     * v10优化：提取最小年限 - 更精确的解析
     */
    private int extractMinYears(String t) {
        if (t == null || t.isEmpty()) return 0;
        
        // 匹配 "N-M年" 中的 N
        Matcher m = Pattern.compile("(\\d+)\\s*[-~到至]\\s*\\d+\\s*年").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        
        // 匹配 "N年以上"
        m = Pattern.compile("(\\d+)\\s*年以上").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        
        // 匹配 "N年" 模式
        m = Pattern.compile("(\\d+)\\s*年").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        
        return 0;
    }

    /**
     * v10优化：提取最大年限 - 处理范围值
     */
    private int extractMaxYears(String t) {
        if (t == null || t.isEmpty()) return 0;
        
        // 匹配 "N-M年" 中的 M
        Matcher m = Pattern.compile("\\d+\\s*[-~到至]\\s*(\\d+)\\s*年").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        
        // 无范围时返回0（表示无上限）
        return 0;
    }

    private int getEduLevel(String edu) {
        if (edu == null) return 0;
        // 先检查"在读"状态，如果有"在读"则降一级
        boolean isStudying = edu.contains("在读");
        String[] keys = {"博士","硕士","研究生","本科","学士","大专","专科","高中","中专"};
        int[] vals = {5,4,4,3,3,2,2,1,1};
        for (int i = 0; i < keys.length; i++) {
            if (edu.contains(keys[i])) {
                int level = vals[i];
                // "在读"状态降一级（如"本科在读"按大专对待）
                if (isStudying && level > 1) level--;
                return level;
            }
        }
        return 0;
    }

    private String extractCity(String t) {
        if (t == null) return null;
        for (String c : CITIES) if (t.contains(c)) return c;
        return null;
    }

    private boolean isRelatedCategory(Long a, Long b) {
        Set<Long> r = RELATED_CATEGORIES.get(a);
        return r != null && r.contains(b);
    }

    private String safe(String s) { return s != null ? s : ""; }
    private String nvl(String s, String d) { return (s != null && !s.isEmpty()) ? s : d; }

    private String formatSalaryRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return "面议";
        if (min == null) return "¥" + max + "以内";
        if (max == null) return "¥" + min + "以上";
        return "¥" + min + "-¥" + max;
    }

    private double round(double v) {
        return new BigDecimal(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "{}"; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDetail(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }

    // ==================== 缓存序列化辅助 ====================

    /**
     * 将职位推荐列表转为可安全存入 Redis 的纯 Map 列表。
     * 把 Job 实体转为只含前端所需字段的 Map，避免 LocalDateTime 序列化问题。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toCacheableJobList(List<Map<String, Object>> recommendations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> rec : recommendations) {
            Map<String, Object> cacheRec = new HashMap<>(rec);
            Job job = (Job) rec.get("job");
            if (job != null) {
                cacheRec.put("job", jobToMap(job));
            }
            result.add(cacheRec);
        }
        return result;
    }

    /**
     * 将候选人推荐列表转为可安全存入 Redis 的纯 Map 列表。
     * 把 Resume 实体转为只含前端所需字段的 Map，避免 LocalDateTime 序列化问题。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toCacheableCandidateList(List<Map<String, Object>> recommendations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> rec : recommendations) {
            Map<String, Object> cacheRec = new HashMap<>(rec);
            Resume resume = (Resume) rec.get("resume");
            if (resume != null) {
                cacheRec.put("resume", resumeToMap(resume));
            }
            result.add(cacheRec);
        }
        return result;
    }

    /**
     * Job 实体 → 只含前端展示所需字段的 Map
     */
    private Map<String, Object> jobToMap(Job job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("description", job.getDescription());
        map.put("requirements", job.getRequirements());
        map.put("salaryMin", job.getSalaryMin() != null ? job.getSalaryMin().toPlainString() : null);
        map.put("salaryMax", job.getSalaryMax() != null ? job.getSalaryMax().toPlainString() : null);
        map.put("location", job.getLocation());
        map.put("experienceRequired", job.getExperienceRequired());
        map.put("educationRequired", job.getEducationRequired());
        map.put("categoryId", job.getCategoryId());
        map.put("employerId", job.getEmployerId());
        map.put("status", job.getStatus());
        map.put("createTime", job.getCreateTime() != null ? job.getCreateTime().toString() : null);
        return map;
    }

    /**
     * Resume 实体 → 只含前端展示所需字段的 Map
     */
    private Map<String, Object> resumeToMap(Resume resume) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", resume.getId());
        map.put("name", resume.getName());
        map.put("age", resume.getAge());
        map.put("education", resume.getEducation());
        map.put("skills", resume.getSkills());
        map.put("experience", resume.getExperience());
        map.put("workExperience", resume.getWorkExperience());
        map.put("selfIntroduction", resume.getSelfIntroduction());
        map.put("expectedSalary", resume.getExpectedSalary() != null ? resume.getExpectedSalary().toPlainString() : null);
        map.put("categoryId", resume.getCategoryId());
        map.put("userId", resume.getUserId());
        map.put("createTime", resume.getCreateTime() != null ? resume.getCreateTime().toString() : null);
        return map;
    }

    @Override
    public long countByDateRange(LocalDateTime start, LocalDateTime end) {
        return lambdaQuery().ge(MatchRecord::getCreateTime, start).lt(MatchRecord::getCreateTime, end).count();
    }
    
    // ==================== v10新增：性能优化方法 ====================
    
    /**
     * 缓存预热 - 在系统启动时或低峰期调用
     * 预加载热门职位的技能权重到内存缓存
     */
    public void warmUpCache() {
        if (cacheWarmed) {
            log.info("缓存已预热，跳过");
            return;
        }
        
        log.info("开始缓存预热...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 预加载最近的100个活跃职位
            List<Job> recentJobs = jobService.lambdaQuery()
                .eq(Job::getStatus, 1) // 活跃状态
                .orderByDesc(Job::getCreateTime)
                .last("LIMIT 100")
                .list();
            
            int warmedCount = 0;
            for (Job job : recentJobs) {
                // 触发技能权重计算并缓存
                extractSkillWeights(job);
                warmedCount++;
            }
            
            cacheWarmed = true;
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("缓存预热完成: 预热职位数={}, 耗时={}ms", warmedCount, elapsed);
            
        } catch (Exception e) {
            log.error("缓存预热失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理过期缓存 - 定时任务调用
     */
    public void clearExpiredCache() {
        synchronized (skillWeightsCache) {
            int beforeSize = skillWeightsCache.size();
            // 简单策略：清空一半缓存
            int toRemove = beforeSize / 2;
            Iterator<String> iterator = skillWeightsCache.keySet().iterator();
            for (int i = 0; i < toRemove && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
            log.info("缓存清理完成: 清理前={}, 清理后={}", beforeSize, skillWeightsCache.size());
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("skillWeightsCacheSize", skillWeightsCache.size());
        stats.put("skillWeightsCacheMaxSize", SKILL_WEIGHTS_CACHE_MAX_SIZE);
        stats.put("cacheWarmed", cacheWarmed);
        stats.put("keywordsCached", cachedJobKeywords != null);
        return stats;
    }
}
