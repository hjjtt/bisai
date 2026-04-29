package com.bisai.service;

import com.bisai.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ModelScope API 客户端（OpenAI 兼容接口）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelScopeClient {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 调用 Chat Completion API
     */
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, aiConfig.getTemperature());
    }

    /**
     * 调用 Chat Completion API（自定义温度）
     */
    public String chat(String systemPrompt, String userMessage, double temperature) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", aiConfig.getModel());
            body.put("max_tokens", aiConfig.getMaxTokens());
            body.put("temperature", temperature);

            ArrayNode messages = body.putArray("messages");
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiConfig.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            log.info("调用 ModelScope API, model={}, 消息长度={}", aiConfig.getModel(), userMessage.length());
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("ModelScope API 调用失败: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("AI 服务调用失败: HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            // 记录 token 使用情况
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                log.info("Token 使用: input={}, output={}, total={}",
                        usage.path("prompt_tokens").asInt(),
                        usage.path("completion_tokens").asInt(),
                        usage.path("total_tokens").asInt());
            }

            return content;

        } catch (Exception e) {
            log.error("调用 ModelScope API 异常: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用异常: " + e.getMessage());
        }
    }

    /**
     * 调用 Chat Completion API 并解析 JSON 响应
     */
    public JsonNode chatAsJson(String systemPrompt, String userMessage) {
        String content = chat(systemPrompt, userMessage);
        return parseJsonResponse(content);
    }

    /**
     * 测试连通性
     */
    public boolean testConnection() {
        try {
            String result = chat("你是一个测试助手。", "请回复：连接成功");
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            log.warn("模型连通性测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 AI 回复中提取 JSON
     */
    private JsonNode parseJsonResponse(String content) {
        try {
            // 尝试直接解析
            return objectMapper.readTree(content);
        } catch (Exception e1) {
            try {
                // 尝试提取 markdown 代码块中的 JSON
                String json = content;
                if (json.contains("```json")) {
                    json = json.substring(json.indexOf("```json") + 7);
                    json = json.substring(0, json.indexOf("```"));
                } else if (json.contains("```")) {
                    json = json.substring(json.indexOf("```") + 3);
                    json = json.substring(0, json.indexOf("```"));
                }
                json = json.trim();
                return objectMapper.readTree(json);
            } catch (Exception e2) {
                log.warn("解析 AI JSON 响应失败: {}", content);
                throw new RuntimeException("AI 返回格式异常，无法解析 JSON");
            }
        }
    }
}
