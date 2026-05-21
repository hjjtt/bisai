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
            "ai.api-url", "ai.max-tokens", "ai.daily-token-limit", "ai.daily-call-limit"
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
            map.put(key, value);
        });
        return Result.ok(map);
    }

    public Result<Void> updateConfig(Map<String, String> configMap) {
        configMap.forEach((key, value) -> {
            if (!ALLOWED_CONFIG_KEYS.contains(key)) {
                log.warn("拒绝修改未授权的配置项: {}", key);
                return;
            }
            SystemConfig existing = configMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key)
            );
            if (existing != null) {
                existing.setConfigValue(value);
                configMapper.updateById(existing);
            } else {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                configMapper.insert(config);
            }
        });
        return Result.ok();
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return SENSITIVE_KEY_PATTERNS.stream().anyMatch(lower::contains);
    }

    /**
     * 测试模型连通性 - 调用 ModelScope API 实际请求
     */
    public Result<Map<String, Object>> testModelConnection(String apiUrl, String apiKey) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 优先使用传入的参数，否则使用默认配置
            String testKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : aiConfig.getApiKey();

            if (testKey == null || testKey.isEmpty()) {
                result.put("success", false);
                result.put("message", "API Key 未配置");
                return Result.ok(result);
            }

            // 使用当前配置的客户端进行测试
            boolean connected = modelScopeClient.testConnection();

            result.put("success", connected);
            result.put("message", connected ? "模型连接测试成功，AI 服务可用" : "模型连接测试失败，请检查 API Key 和网络");
            return Result.ok(result);

        } catch (Exception e) {
            log.error("模型连通性测试异常: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            return Result.ok(result);
        }
    }
}
