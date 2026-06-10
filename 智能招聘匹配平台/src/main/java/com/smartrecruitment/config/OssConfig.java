package com.smartrecruitment.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ================================================
 * 阿里云 OSS 配置
 * ================================================
 *
 * 功能说明：
 *   - 配置阿里云 OSS 客户端
 *   - 通过 oss.enabled=true 开启 OSS 存储（默认 false 使用本地存储）
 *   - application.yml 中配置 accessKeyId / accessKeySecret / endpoint / bucketName
 *
 * 使用方式：
 *   1. 先在阿里云控制台创建 Bucket
 *   2. 获取 AccessKey ID 和 Secret
 *   3. 修改 application.yml 中的 oss 配置
 *   4. 设置 oss.enabled=true 切换到云端存储
 *
 * 如需扩展：
 *   - 添加自定义域名绑定（CDN 加速）
 *   - 添加文件访问权限控制（公共读/私有读）
 *   - 添加跨域规则配置
 *   - 添加生命周期管理（自动清理过期文件）
 */
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    @Bean
    @ConditionalOnProperty(name = "oss.enabled", havingValue = "true")
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
}
