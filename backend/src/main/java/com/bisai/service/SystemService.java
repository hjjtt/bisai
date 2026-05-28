package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.config.AiConfig;
import com.bisai.entity.SystemConfig;
import com.bisai.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemConfigMapper configMapper;
    private final AiConfig aiConfig;
    private final ModelScopeClient modelScopeClient;

    private static final Set<String> ALLOWED_CONFIG_KEYS = Set.of(
            "ai.api-key", "ai.chat-model", "ai.embedding-model", "ai.rerank-model",
            "ai.api-url", "ai.max-tokens", "ai.daily-token-limit", "ai.daily-call-limit",
            "ai.temperature", "ai.timeout"
    );

    // 前端字段名 → DB key 映射
    private static final Map<String, String> FRONTEND_TO_DB_KEY = Map.of(
            "textModelApiUrl", "ai.api-url",
            "textModelApiKey", "ai.api-key",
            "model", "ai.chat-model",
            "maxTokens", "ai.max-tokens",
            "temperature", "ai.temperature",
            "timeout", "ai.timeout"
    );

    // DB key → 前端字段名 映射（反向）
    private static final Map<String, String> DB_TO_FRONTEND_KEY = Map.of(
            "ai.api-url", "textModelApiUrl",
            "ai.api-key", "textModelApiKey",
            "ai.chat-model", "model",
            "ai.max-tokens", "maxTokens",
            "ai.temperature", "temperature",
            "ai.timeout", "timeout"
    );

    private static final Set<String> SENSITIVE_KEY_PATTERNS = Set.of(
            "api-key", "apikey", "password", "secret", "token"
    );

    public Result<Map<String, String>> getConfig() {
        List<SystemConfig> configs = configMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        configs.forEach(c -> {
            String key = c.getConfigKey();
            String value = c.getConfigValue();
            if (isSensitiveKey(key) && value != null && value.length() > 4) {
                value = "****" + value.substring(value.length() - 4);
            }
            // DB key 映射为前端字段名
            String frontendKey = DB_TO_FRONTEND_KEY.getOrDefault(key, key);
            map.put(frontendKey, value);
        });
        return Result.ok(map);
    }

    public Result<Void> updateConfig(Map<String, String> configMap) {
        Map<String, String> dbUpdates = new HashMap<>();
        configMap.forEach((key, value) -> {
            // 前端字段名映射为 DB key
            String dbKey = FRONTEND_TO_DB_KEY.getOrDefault(key, key);
            if (!ALLOWED_CONFIG_KEYS.contains(dbKey)) {
                log.warn("拒绝修改未授权的配置项: {}", key);
                return;
            }
            if (value == null || value.isEmpty()) return;
            SystemConfig existing = configMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, dbKey)
            );
            if (existing != null) {
                existing.setConfigValue(value);
                configMapper.updateById(existing);
            } else {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(dbKey);
                config.setConfigValue(value);
                configMapper.insert(config);
            }
            dbUpdates.put(dbKey, value);
        });
        // 刷新运行时 AiConfig Bean
        refreshAiConfig(dbUpdates);
        return Result.ok();
    }

    private void refreshAiConfig(Map<String, String> updates) {
        updates.forEach((dbKey, value) -> {
            switch (dbKey) {
                case "ai.chat-model" -> aiConfig.setModel(value);
                case "ai.api-url" -> aiConfig.setBaseUrl(value);
                case "ai.api-key" -> aiConfig.setApiKey(value);
                case "ai.max-tokens" -> aiConfig.setMaxTokens(Integer.parseInt(value));
                case "ai.temperature" -> aiConfig.setTemperature(Double.parseDouble(value));
                case "ai.timeout" -> aiConfig.setTimeout(Integer.parseInt(value));
            }
        });
        log.info("AiConfig 已刷新: model={}, baseUrl={}, maxTokens={}, temperature={}, timeout={}",
                aiConfig.getModel(), aiConfig.getBaseUrl(), aiConfig.getMaxTokens(),
                aiConfig.getTemperature(), aiConfig.getTimeout());
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return SENSITIVE_KEY_PATTERNS.stream().anyMatch(lower::contains);
    }

    /**
     * 测试模型连通性 - 调用 ModelScope API 实际请求
     */
    public Result<Map<String, Object>> testModelConnection(String apiUrl, String apiKey, String model) {
        Map<String, Object> result = new HashMap<>();

        try {
            String testKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : aiConfig.getApiKey();
            String testUrl = (apiUrl != null && !apiUrl.isEmpty()) ? apiUrl : aiConfig.getBaseUrl();
            String testModel = (model != null && !model.isEmpty()) ? model : aiConfig.getModel();

            if (testKey == null || testKey.isEmpty()) {
                result.put("success", false);
                result.put("message", "API Key 未配置");
                return Result.ok(result);
            }

            boolean connected = modelScopeClient.testConnection(testModel, testUrl, testKey);

            result.put("success", connected);
            result.put("message", connected
                    ? "模型连接测试成功（" + testModel + "），AI 服务可用"
                    : "模型连接测试失败，请检查配置和网络");
            return Result.ok(result);

        } catch (Exception e) {
            log.error("模型连通性测试异常: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            return Result.ok(result);
        }
    }
}
