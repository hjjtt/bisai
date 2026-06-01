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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 记录今日配额已耗尽的模型，key=模型名，value=耗尽日期 */
    private final ConcurrentHashMap<String, LocalDate> quotaExhausted = new ConcurrentHashMap<>();

    /**
     * 检查模型今日配额是否已耗尽
     */
    private boolean isModelExhausted(String model) {
        LocalDate exhaustedDate = quotaExhausted.get(model);
        return exhaustedDate != null && exhaustedDate.equals(LocalDate.now());
    }

    /**
     * 标记模型今日配额已耗尽
     */
    private void markModelExhausted(String model) {
        quotaExhausted.put(model, LocalDate.now());
        log.warn("模型 {} 今日配额已耗尽，后续调用将自动切换备用模型", model);
    }

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
        List<String> models = getFallbackChain();
        Exception lastError = null;

        for (String model : models) {
            if (isModelExhausted(model)) {
                log.info("模型 {} 配额已耗尽，跳过", model);
                continue;
            }
            try {
                return doChat(systemPrompt, userMessage, temperature, model);
            } catch (Exception e) {
                lastError = e;
                if (!(e instanceof RateLimitException)) {
                    log.warn("模型 {} 调用失败: {}", model, e.getMessage());
                }
            }
        }

        if (lastError instanceof RateLimitException) {
            throw (RateLimitException) lastError;
        }
        throw lastError != null ? new RuntimeException("所有模型均调用失败: " + lastError.getMessage())
                : new RuntimeException("所有模型今日配额已用完，请明天再试");
    }

    /**
     * 获取备用模型链（主模型 + 备用模型列表，去重）
     */
    private List<String> getFallbackChain() {
        List<String> chain = new ArrayList<>();
        chain.add(aiConfig.getModel());
        String fallbacks = aiConfig.getFallbackModels();
        if (fallbacks != null && !fallbacks.isEmpty()) {
            for (String m : fallbacks.split(",")) {
                String trimmed = m.trim();
                if (!trimmed.isEmpty() && !chain.contains(trimmed)) {
                    chain.add(trimmed);
                }
            }
        }
        return chain;
    }

    /**
     * 实际调用 Chat Completion API（可指定模型）
     */
    private String doChat(String systemPrompt, String userMessage, double temperature, String model) {
        int estimatedInputTokens = estimateTokens(systemPrompt) + estimateTokens(userMessage);
        aiUsageService.checkQuota(estimatedInputTokens);
        try {
            // Qwen3 模型：用 /no_think 禁用思考链，节省 token 和时间
            if (model != null && model.contains("Qwen")) {
                systemPrompt = "/no_think\n" + (systemPrompt != null ? systemPrompt : "");
            }

            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(new UserMessage(java.util.Objects.requireNonNull(userMessage, "userMessage cannot be null")));

            log.info("调用 ModelScope API, model={}, 消息长度={}", model, userMessage.length());
            ChatResponse response = chatModel.call(new Prompt(
                    messages,
                    OpenAiChatOptions.builder()
                            .model(model)
                            .maxTokens(aiConfig.getMaxTokens())
                            .temperature(temperature)
                            .build()
            ));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("AI 返回空响应, model={}, response={}", model, response);
                aiUsageService.record(model, "CHAT", estimatedInputTokens, 0, false, "AI 返回空响应");
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
            aiUsageService.record(model, "CHAT", inputTokens, outputTokens, true, null);

            return content;

        } catch (Exception e) {
            aiUsageService.record(model, "CHAT", estimatedInputTokens, 0, false, e.getMessage());
            // 429 限流：标记模型耗尽，友好提示
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                markModelExhausted(model);
                throw new RateLimitException("今日 AI 调用额度已用完，请明天再试，或联系管理员切换模型");
            }
            log.error("调用 ModelScope API 异常: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用异常: " + e.getMessage());
        }
    }

    /**
     * 调用 Chat Completion API，并支持传入 Tool (Function Calling)
     */
    public String chatWithTools(String systemPrompt, String userMessage, java.util.List<String> toolNames) {
        List<String> models = getFallbackChain();
        Exception lastError = null;

        for (String model : models) {
            if (isModelExhausted(model)) {
                log.info("模型 {} 配额已耗尽，跳过", model);
                continue;
            }
            try {
                return doChatWithTools(systemPrompt, userMessage, toolNames, model);
            } catch (Exception e) {
                lastError = e;
                if (!(e instanceof RateLimitException)) {
                    log.warn("模型 {} Agent 调用失败: {}", model, e.getMessage());
                }
            }
        }

        if (lastError instanceof RateLimitException) {
            throw (RateLimitException) lastError;
        }
        throw lastError != null ? new RuntimeException("所有模型均 Agent 调用失败: " + lastError.getMessage())
                : new RuntimeException("所有模型今日配额已用完，请明天再试");
    }

    /**
     * 实际调用 Agent API（可指定模型）
     */
    private String doChatWithTools(String systemPrompt, String userMessage, java.util.List<String> toolNames, String model) {
        int estimatedInputTokens = estimateTokens(systemPrompt) + estimateTokens(userMessage);
        aiUsageService.checkQuota(estimatedInputTokens);
        try {
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(new UserMessage(java.util.Objects.requireNonNull(userMessage, "userMessage cannot be null")));

            log.info("调用 Agentic API (带有工具), model={}, 工具={}", model, toolNames);

            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .model(model)
                    .maxTokens(aiConfig.getMaxTokens())
                    .temperature(aiConfig.getTemperature());

            if (toolNames != null && !toolNames.isEmpty()) {
                optionsBuilder.toolNames(new java.util.HashSet<>(toolNames));
            }

            ChatResponse response = chatModel.call(new Prompt(messages, optionsBuilder.build()));

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("AI (带有工具) 返回空响应");
                throw new RuntimeException("AI 服务返回空响应，请重试");
            }

            return response.getResult().getOutput().getText();

        } catch (Exception e) {
            aiUsageService.record(model, "AGENT", estimatedInputTokens, 0, false, e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                markModelExhausted(model);
                throw new RateLimitException("今日 AI 调用额度已用完，请明天再试");
            }
            log.error("调用 Agentic API 异常: {}", e.getMessage(), e);
            throw new RuntimeException("Agent 服务调用异常: " + e.getMessage());
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
        return chatAsJson(systemPrompt, userMessage, aiConfig.getTemperature());
    }

    public JsonNode chatAsJson(String systemPrompt, String userMessage, double temperature) {
        // 自动追加 JSON-only 指令，避免每个 prompt 重复写
        String enhancedPrompt = systemPrompt + "\n只返回 JSON，不要其他内容。";
        String content = chat(enhancedPrompt, userMessage, temperature);
        return parseJsonResponse(content);
    }

    /**
     * 测试连通性（使用当前配置）
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
     * 测试连通性（使用临时参数）
     */
    public boolean testConnection(String model, String apiUrl, String apiKey) {
        String testModel = model != null ? model : aiConfig.getModel();
        try {
            OpenAiChatOptions testOptions = OpenAiChatOptions.builder()
                    .model(testModel)
                    .maxTokens(100)
                    .temperature(0.1)
                    .build();
            ChatResponse response = chatModel.call(new Prompt(
                    List.of(new SystemMessage("你是一个测试助手。"), new org.springframework.ai.chat.messages.UserMessage("请回复：连接成功")),
                    testOptions
            ));
            boolean success = response != null && response.getResult() != null
                    && response.getResult().getOutput() != null
                    && !response.getResult().getOutput().getText().isEmpty();
            int inputTokens = estimateTokens("你是一个测试助手。请回复：连接成功");
            int outputTokens = success ? estimateTokens(response.getResult().getOutput().getText()) : 0;
            aiUsageService.record(testModel, "TEST", inputTokens, outputTokens, success, success ? null : "AI 返回空响应");
            return success;
        } catch (Exception e) {
            int inputTokens = estimateTokens("你是一个测试助手。请回复：连接成功");
            aiUsageService.record(testModel, "TEST", inputTokens, 0, false, e.getMessage());
            log.warn("模型连通性测试失败(model={}): {}", testModel, e.getMessage());
            return false;
        }
    }

    /**
     * 从 AI 回复中提取 JSON
     */
    private JsonNode parseJsonResponse(String content) {
        // 先剥离 Qwen3 等模型的思维链标签
        String clean = stripThinkingTags(content);
        try {
            return objectMapper.readTree(clean);
        } catch (Exception e1) {
            try {
                String json = clean.trim();
                // 剥离 markdown 代码块
                if (json.contains("```json")) {
                    int start = json.indexOf("```json") + 7;
                    int end = json.indexOf("```", start);
                    json = end > start ? json.substring(start, end) : json.substring(start);
                } else if (json.contains("```")) {
                    int start = json.indexOf("```") + 3;
                    int end = json.indexOf("```", start);
                    json = end > start ? json.substring(start, end) : json.substring(start);
                }
                json = json.trim();
                // 如果不是以 { 或 [ 开头，尝试提取首个 JSON 结构
                if (!json.startsWith("{") && !json.startsWith("[")) {
                    int start = json.indexOf('{');
                    if (start < 0) start = json.indexOf('[');
                    if (start >= 0) json = json.substring(start);
                }
                return objectMapper.readTree(json);
            } catch (Exception e2) {
                // 最后尝试：用正则提取最后一个完整的 JSON 对象（处理 AI 在 JSON 后追加说明文字的情况）
                try {
                    String extracted = extractLastJsonObject(clean);
                    if (extracted != null) {
                        return objectMapper.readTree(extracted);
                    }
                } catch (Exception ignored) {}
                log.warn("解析 AI JSON 响应失败, 原始长度={}, 剥离后前500字: {}",
                        content != null ? content.length() : 0,
                        clean != null ? clean.substring(0, Math.min(clean.length(), 500)) : "null");
                throw new RuntimeException("AI 返回格式异常，无法解析 JSON");
            }
        }
    }

    /**
     * 提取字符串中最后一个完整的 JSON 对象（匹配最外层花括号）
     */
    private String extractLastJsonObject(String text) {
        int end = text.lastIndexOf('}');
        if (end < 0) return null;
        // 从 end 向前找匹配的起始 {
        int depth = 0;
        for (int i = end; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') depth--;
            if (depth == 0) {
                return text.substring(i, end + 1);
            }
        }
        return null;
    }

    /**
     * 剥离 Qwen3 等模型的思维链标签 <think>...</think>
     */
    private String stripThinkingTags(String content) {
        if (content == null) return null;
        // 支持带换行和不带换行的 think 标签
        return content.replaceAll("(?s)<think>.*?</think>", "").trim();
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
