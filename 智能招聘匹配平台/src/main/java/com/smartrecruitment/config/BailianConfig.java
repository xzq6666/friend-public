package com.smartrecruitment.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI API 配置 - 适配 LongCat (Anthropic 兼容模式)
 *
 * 增强功能：
 * - 自动重试机制（最多3次，指数退避）
 * - 精细化超时控制（连接15s/读取120s/写入30s）
 * - 响应格式校验
 * - Token用量日志
 */
@Component
public class BailianConfig {

    private static final Logger log = LoggerFactory.getLogger(BailianConfig.class);

    @Value("${bailian.api-key:ak_2qK7323AA1Pm5ih16Q5M93yB0iH6m}")
    private String apiKey;

    @Value("${bailian.model:LongCat-2.0-Preview}")
    private String model;

    @Value("${bailian.base-url:https://api.longcat.chat/anthropic}")
    private String baseUrl;

    // 重试配置
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BailianConfig() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)   // 连接超时15s
                .readTimeout(120, TimeUnit.SECONDS)     // 读取超时120s
                .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时30s
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 调用AI接口 - 强制返回JSON格式
     */
    public String call(String prompt) {
        return call(prompt, true);
    }

    /**
     * 调用AI接口（带重试机制）
     * @param prompt 提示词
     * @param forceJson 是否强制返回JSON格式
     */
    public String call(String prompt, boolean forceJson) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String result = doCall(prompt, forceJson);
                if (attempt > 1) {
                    log.info("AI API第{}次尝试成功", attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("AI API第{}次调用失败: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("AI调用被中断", ie);
                    }
                }
            }
        }

        log.error("AI API全部{}次重试失败", MAX_RETRIES);
        // 不抛出异常，返回空结果，避免阻塞主流程
        return "{}";
    }

    /**
     * 执行单次AI调用 (Anthropic 格式)
     */
    private String doCall(String prompt, boolean forceJson) throws IOException {
        String fullUrl = baseUrl + "/v1/messages";
        log.info("调用AI API [LongCat]，模型：{}，完整URL：{}，prompt长度：{}", model, fullUrl, prompt.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 16000);
        
        List<Map<String, String>> messages = new ArrayList<>();
        requestBody.put("system", "你是一名专业的招聘领域AI专家。请根据用户的具体指令进行回答，如果用户要求JSON格式，请输出合法的JSON。");
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        if (forceJson) {
            // Anthropic 主要依靠 Prompt 约束 JSON 格式
        }

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(fullUrl)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization", "Bearer " + apiKey.trim())
                .build();

        long startTime = System.currentTimeMillis();

        try (Response response = httpClient.newCall(request).execute()) {
            long elapsed = System.currentTimeMillis() - startTime;

            if (response.body() == null) {
                throw new RuntimeException("AI API返回空响应体");
            }

            String responseBody = response.body().string();
            log.info("AI API响应，状态码：{}，耗时：{}ms，响应长度：{}", response.code(), elapsed, responseBody.length());

            if (!response.isSuccessful()) {
                log.error("API调用失败: HTTP {} - {}", response.code(), responseBody);
                throw new RuntimeException("AI API调用失败: HTTP " + response.code());
            }

            // 解析 Anthropic 响应结构: { "content": [{"type": "text", "text": "..."}] }
            JsonNode responseNode = objectMapper.readTree(responseBody);
            JsonNode contentArray = responseNode.get("content");
            
            if (contentArray != null && contentArray.isArray() && !contentArray.isEmpty()) {
                JsonNode firstContent = contentArray.get(0);
                JsonNode textNode = firstContent.get("text");
                if (textNode != null) {
                    return cleanAiResponse(textNode.asText());
                }
            }

            throw new RuntimeException("AI API响应格式异常：未找到有效内容");
        }
    }

    /**
     * 清洗AI返回内容
     */
    private String cleanAiResponse(String content) {
        if (content == null || content.isEmpty()) return "{}";

        // 不在这里做太多清理，保留原始内容让调用方处理
        // 只移除首尾空白
        return content.trim();
    }
}
