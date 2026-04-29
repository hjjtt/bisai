package com.bisai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bisai.config.AiConfig;
import com.bisai.entity.AiCallLog;
import com.bisai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiCallLogMapper aiCallLogMapper;
    private final AiConfig aiConfig;

    public void checkQuota(int estimatedInputTokens) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        LambdaQueryWrapper<AiCallLog> today = new LambdaQueryWrapper<AiCallLog>()
                .ge(AiCallLog::getCreatedAt, start)
                .lt(AiCallLog::getCreatedAt, end);

        Long callCount = aiCallLogMapper.selectCount(today);
        if (callCount >= aiConfig.getDailyCallLimit()) {
            throw new RuntimeException("AI 今日调用次数已达到上限");
        }

        int usedTokens = aiCallLogMapper.selectList(today).stream()
                .map(AiCallLog::getTotalTokens)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();
        if (usedTokens + estimatedInputTokens > aiConfig.getDailyTokenLimit()) {
            throw new RuntimeException("AI 今日 Token 用量已达到上限");
        }
    }

    public void record(String model, String callType, int inputTokens, int outputTokens, boolean success, String errorMessage) {
        AiCallLog log = new AiCallLog();
        log.setModel(model);
        log.setCallType(callType);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setTotalTokens(inputTokens + outputTokens);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());
        aiCallLogMapper.insert(log);
    }
}
