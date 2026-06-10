package com.smartrecruitment.config;

import com.smartrecruitment.service.SkillGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时自动初始化技能图谱数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private SkillGraphService skillGraphService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化技能图谱数据...");
        try {
            // 检查是否已初始化
            long count = skillGraphService.count();
            if (count > 0) {
                log.info("技能图谱数据已存在，跳过初始化。当前技能数量: {}", count);
                return;
            }

            // 初始化技能图谱数据
            skillGraphService.initSkillGraphData();
            log.info("技能图谱数据初始化完成，共初始化 {} 个技能", skillGraphService.count());
        } catch (Exception e) {
            log.error("技能图谱数据初始化失败: {}", e.getMessage(), e);
            log.warn("请确保数据库表 skill_graph 和 skill_relation 已创建。可执行 update.sql 脚本。");
        }
    }
}
