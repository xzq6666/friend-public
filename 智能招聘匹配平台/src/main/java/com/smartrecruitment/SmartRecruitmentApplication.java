package com.smartrecruitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ================================================
 * 智能招聘管理系统 - 主启动类
 * ================================================
 *
 * 功能入口：
 *   - 启动整个 SpringBoot 应用
 *   - 自动扫描所有 @Component, @Service, @Controller, @Mapper
 *
 * 如需扩展：
 *   - 添加 @EnableScheduling 开启定时任务
 *   - 添加 @EnableTransactionManagement 开启事务管理
 *   - 添加 @EnableSwagger2 开启 API 文档
 */
@SpringBootApplication
@EnableCaching      // 启用缓存（基于 Redis）
@EnableAsync        // 启用异步调用（用于 AI 分析等耗时操作）
@EnableScheduling   // 启用定时任务（用于订阅推送等）
public class SmartRecruitmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartRecruitmentApplication.class, args);
    }
}
