package com.bisai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String baseUrl = "https://api-inference.modelscope.cn/v1";
    private String apiKey;
    private String model = "Qwen/Qwen2.5-72B-Instruct";
    private String embeddingModel = "damo/nlp_corom_sentence-embedding_chinese-base";
    private String visionModel = "Qwen/Qwen3.5-35B-A3B";
    private int maxTokens = 4096;
    private double temperature = 0.3;
    private int dailyTokenLimit = 200000;
    private int dailyCallLimit = 1000;
}
