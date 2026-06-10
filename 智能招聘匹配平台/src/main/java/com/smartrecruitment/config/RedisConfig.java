package com.smartrecruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * ================================================
 * Redis 缓存配置
 * ================================================
 *
 * 功能说明：
 *   - 配置 RedisTemplate 的序列化方式
 *   - Key 使用 String 序列化（可读性好）
 *   - Value 使用 JSON 序列化（兼容性强）
 *   - 支持存储对象、List、Map 等复杂类型
 *   - 支持 Java 8 日期时间类型（LocalDateTime 等）
 *
 * 如需扩展：
 *   - 添加 RedisCacheManager Bean 自定义缓存注解行为
 *   - 配置连接池参数（已在 application.yml 中配置）
 *   - 添加多个 RedisTemplate 针对不同数据类型
 *   - 添加 Redis 分布式锁工具
 *   - 配置 Key 前缀命名空间隔离
 */
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 配置支持 Java 8 日期时间类型的 ObjectMapper
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 日期时间类型
        mapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳，改为 ISO-8601 字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了 JavaTimeModule 的 ObjectMapper，支持 LocalDateTime 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
