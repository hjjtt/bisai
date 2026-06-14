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
    private String model = "stepfun-ai/Step-3.7-Flash";
    /** 备用模型链：主模型失败时按顺序尝试，用逗号分隔 */
    private String fallbackModels = "stepfun-ai/Step-3.5-Flash,deepseek-ai/DeepSeek-V4-Flash,Qwen/Qwen3.5-35B-A3B";
    private String embeddingModel = "damo/nlp_corom_sentence-embedding_chinese-base";
    private String visionModel = "Qwen/Qwen3.5-35B-A3B";
    private int maxTokens = 4096;
    private double temperature = 0.3;
    private int dailyTokenLimit = 200000;
    private int dailyCallLimit = 1000;
    /** API 调用超时（秒） */
    private int timeout = 60;
    /** 是否使用 Agent 模式评分（false 则走传统单次调用） */
    private boolean useAgentScore = false;

    // ==================== LLM-as-a-Judge 配置 ====================

    /** 多轮采样次数（默认 3 轮，取中位数） */
    private int judgeRounds = 3;
    /** 多轮采样温度（建议 0.3，略高于单次的 0.1 以增加多样性） */
    private double judgeTemperature = 0.3;
    /** 冗长偏差惩罚阈值（字数超过此值开始微调，默认 5000 字） */
    private int verbosityPenaltyThreshold = 5000;
    /** 冗长偏差惩罚率（每超过 1000 字扣 maxScore 的百分比，默认 0.02 即 2%） */
    private double verbosityPenaltyRate = 0.02;
    /** 交叉模型偏差阈值（两模型评分差超过此值标记需人工审核，默认 15 分） */
    private double crossModelDivergenceThreshold = 15.0;
    /** 是否启用交叉模型评估（用备用模型独立评分，比较偏差） */
    private boolean enableCrossModel = false;
    /** 是否启用规则预评分（基于指标名关键词检测缺失章节，命中直接 0 分）。
     *  规则预评分依赖指标命名约定，命名不规范时建议关闭，避免误伤。 */
    private boolean rulePreScoreEnabled = true;
}
