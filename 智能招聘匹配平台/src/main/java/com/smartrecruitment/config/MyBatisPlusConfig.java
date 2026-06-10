package com.smartrecruitment.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ================================================
 * MyBatis-Plus 配置
 * ================================================
 *
 * 功能说明：
 *   - 配置 Mapper 扫描路径
 *   - 添加分页插件（支持 MySQL 分页查询）
 *
 * 如需扩展：
 *   - 添加乐观锁插件 OptimisticLockerInnerInterceptor
 *   - 添加防全表更新/删除插件 BlockAttackInnerInterceptor
 *   - 添加 SQL 性能分析插件 IllegalSQLInnerInterceptor
 *   - 添加数据权限插件（按用户/部门过滤）
 *   - 添加动态表名插件 DynamicTableNameInnerInterceptor
 */
@Configuration
@MapperScan("com.smartrecruitment.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
