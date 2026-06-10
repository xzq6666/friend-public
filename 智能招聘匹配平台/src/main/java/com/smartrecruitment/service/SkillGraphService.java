package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.SkillGraph;
import com.smartrecruitment.entity.SkillRelation;

import java.util.List;
import java.util.Map;

/**
 * 技能图谱服务接口
 */
public interface SkillGraphService extends IService<SkillGraph> {

    /**
     * 获取所有技能节点
     */
    List<SkillGraph> getAllSkills();

    /**
     * 获取所有技能关系
     */
    List<SkillRelation> getAllRelations();

    /**
     * 根据分类获取技能
     */
    List<SkillGraph> getSkillsByCategory(String category);

    /**
     * 获取所有行业列表
     */
    List<String> getAllIndustries();

    /**
     * 根据行业获取技能
     */
    List<SkillGraph> getSkillsByIndustry(String industry);

    /**
     * 构建指定行业的个人能力图谱
     */
    Map<String, Object> buildPersonalGraphByIndustry(String skills, String industry);

    /**
     * 构建指定行业的职位能力图谱
     */
    Map<String, Object> buildJobGraphByIndustry(String requirements, String industry);

    /**
     * 构建个人能力图谱数据
     * @param skills 技能列表（逗号分隔）
     * @return ECharts 图谱数据
     */
    Map<String, Object> buildPersonalGraph(String skills);

    /**
     * 构建职位能力图谱数据
     * @param requirements 职位要求
     * @return ECharts 图谱数据
     */
    Map<String, Object> buildJobGraph(String requirements);

    /**
     * 初始化技能图谱基础数据
     */
    void initSkillGraphData();

    /**
     * 从系统真实数据（简历、职位）自动构建技能图谱
     */
    Map<String, Object> autoBuildFromRealData();

    /**
     * 添加技能节点
     */
    SkillGraph addSkill(SkillGraph skill);

    /**
     * 添加技能关系
     */
    SkillRelation addRelation(SkillRelation relation);

    /**
     * 根据当前技能推荐下一步学习技能
     * @param currentSkills 当前技能（逗号分隔）
     * @param targetJobRequirements 目标职位要求
     * @return 推荐技能列表及优先级
     */
    List<Map<String, Object>> recommendSkills(String currentSkills, String targetJobRequirements);

    /**
     * 技能差距分析：对比当前技能与目标职位要求
     * @param currentSkills 当前技能
     * @param targetRequirements 目标职位要求
     * @return 差距分析结果（已掌握、部分掌握、缺失）
     */
    Map<String, Object> analyzeSkillGap(String currentSkills, String targetRequirements);

    /**
     * 获取技能学习路径
     * @param fromSkill 起始技能
     * @param toSkill 目标技能
     * @return 学习路径上的技能列表
     */
    List<SkillGraph> getLearningPath(String fromSkill, String toSkill);

    /**
     * 从简历技能JSON同步新技能到图谱
     * @param skillsJson 技能JSON数组字符串
     * @param categoryId 行业分类ID
     * @return 新增技能数量
     */
    int syncSkillsToGraph(String skillsJson, Long categoryId);
}
