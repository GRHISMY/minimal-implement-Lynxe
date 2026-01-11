package org.example.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DashScopeLLMClient implements LLMClient{
    private static final Logger log = LoggerFactory.getLogger(DashScopeLLMClient.class);
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DashScopeLLMClient(String apiKey) {
        this(apiKey, "qwen-plus");
    }

    public DashScopeLLMClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LLMResponse chat(String prompt) {
        // 构建请求体
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 4096
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    log.error("LLM API 错误: {} - {}", response.code(), errorBody);
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                String content = root.path("choices").path(0).path("message").path("content").asText();
                int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
                int outputTokens = root.path("usage").path("completion_tokens").asInt(0);

                log.debug("LLM 响应: {} (令牌数: 输入={}, 输出={})",
                        content.substring(0, Math.min(100, content.length())),
                        inputTokens, outputTokens);

                return new LLMResponse(content, inputTokens, outputTokens);
            }

        } catch (IOException e) {
            log.error("调用 LLM API 失败: {}", e.getMessage(), e);
            return null;
        }

    }
}
