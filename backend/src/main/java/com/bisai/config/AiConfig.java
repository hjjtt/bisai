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
    private int maxTokens = 4096;
    private double temperature = 0.3;
}
