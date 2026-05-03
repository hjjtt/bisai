package com.bisai.service;

import com.bisai.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ModelScope API 客户端（OpenAI 兼容接口）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelScopeClient {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;
    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

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
        int estimatedInputTokens = estimateTokens(systemPrompt) + estimateTokens(userMessage);
        aiUsageService.checkQuota(estimatedInputTokens);
        try {
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(new UserMessage(java.util.Objects.requireNonNull(userMessage, "userMessage cannot be null")));

            log.info("调用 ModelScope API, model={}, 消息长度={}", aiConfig.getModel(), userMessage.length());
            ChatResponse response = chatModel.call(new Prompt(
                    messages,
                    OpenAiChatOptions.builder()
                            .model(aiConfig.getModel())
                            .maxTokens(aiConfig.getMaxTokens())
                            .temperature(temperature)
                            .build()
            ));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("AI 返回空响应, model={}, response={}", aiConfig.getModel(), response);
                aiUsageService.record(aiConfig.getModel(), "CHAT", estimatedInputTokens, 0, false, "AI 返回空响应");
                throw new RuntimeException("AI 服务返回空响应，请重试");
            }
            String content = response.getResult().getOutput().getText();
            int inputTokens = estimatedInputTokens;
            int outputTokens = estimateTokens(content);
            Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
            if (usage != null) {
                inputTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : inputTokens;
                outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : outputTokens;
                log.info("Token 使用: input={}, output={}, total={}",
                        inputTokens,
                        outputTokens,
                        usage.getTotalTokens() != null ? usage.getTotalTokens() : inputTokens + outputTokens);
            }
            aiUsageService.record(aiConfig.getModel(), "CHAT", inputTokens, outputTokens, true, null);

            return content;

        } catch (Exception e) {
            log.error("调用 ModelScope API 异常: {}", e.getMessage(), e);
            aiUsageService.record(aiConfig.getModel(), "CHAT", estimatedInputTokens, 0, false, e.getMessage());
            throw new RuntimeException("AI 服务调用异常: " + e.getMessage());
        }
    }

    public List<Double> embedding(String input) {
        int estimatedInputTokens = estimateTokens(input);
        aiUsageService.checkQuota(estimatedInputTokens);
        try {
            float[] values = embeddingModel.embed(java.util.Objects.requireNonNull(input, "input cannot be null"));
            List<Double> embedding = Arrays.stream(toDoubleArray(values)).boxed().toList();
            aiUsageService.record(aiConfig.getEmbeddingModel(), "EMBEDDING", estimatedInputTokens, 0, true, null);
            return embedding;
        } catch (Exception e) {
            aiUsageService.record(aiConfig.getEmbeddingModel(), "EMBEDDING", estimatedInputTokens, 0, false, e.getMessage());
            throw new RuntimeException("Embedding 服务调用异常: " + e.getMessage());
        }
    }

    public String analyzeImage(Path imagePath, String mimeType, String prompt) {
        int estimatedInputTokens = estimateTokens(prompt) + 1000;
        aiUsageService.checkQuota(estimatedInputTokens);
        try {
            java.util.Objects.requireNonNull(imagePath, "imagePath cannot be null");
            java.util.Objects.requireNonNull(mimeType, "mimeType cannot be null");
            java.util.Objects.requireNonNull(prompt, "prompt cannot be null");

            UserMessage userMessage = UserMessage.builder()
                    .text(prompt)
                    .media(Media.builder()
                            .mimeType(MimeTypeUtils.parseMimeType(mimeType))
                            .data(new FileSystemResource(imagePath))
                            .build())
                    .build();
            java.util.Objects.requireNonNull(userMessage, "userMessage builder failed");
            ChatResponse response = chatModel.call(new Prompt(
                    userMessage,
                    OpenAiChatOptions.builder()
                            .model(aiConfig.getVisionModel())
                            .maxTokens(aiConfig.getMaxTokens())
                            .temperature(aiConfig.getTemperature())
                            .build()
            ));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("多模态 AI 返回空响应, model={}", aiConfig.getVisionModel());
                aiUsageService.record(aiConfig.getVisionModel(), "VISION", estimatedInputTokens, 0, false, "AI 返回空响应");
                return null;
            }
            String result = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
            int inputTokens = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : estimatedInputTokens;
            int outputTokens = usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : estimateTokens(result);
            aiUsageService.record(aiConfig.getVisionModel(), "VISION", inputTokens, outputTokens, true, null);
            return result;
        } catch (Exception e) {
            aiUsageService.record(aiConfig.getVisionModel(), "VISION", estimatedInputTokens, 0, false, e.getMessage());
            throw new RuntimeException("多模态服务调用异常: " + e.getMessage());
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

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, text.length() / 2);
    }

    private double[] toDoubleArray(float[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }
}
