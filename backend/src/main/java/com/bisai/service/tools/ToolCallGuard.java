package com.bisai.service.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 工具调用防护：防止模型陷入幻觉死循环。
 * 每个 submissionId 维护一个调用计数器，超过阈值则拒绝后续所有工具调用。
 */
@Slf4j
@Component
public class ToolCallGuard {

    /** 每个 submission 最大工具调用次数（4 工具 × 8 指标 + 冗余 ≈ 20） */
    private static final int MAX_CALLS_PER_SUBMISSION = 25;

    private final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * 检查并记录一次工具调用。超过阈值则抛异常中断 Agent 循环。
     *
     * @param submissionId 提交 ID
     * @param toolName     工具名（用于日志）
     * @throws RuntimeException 超过调用上限时抛出
     */
    public void checkAndRecord(Long submissionId, String toolName) {
        if (submissionId == null) return;

        AtomicInteger counter = counters.computeIfAbsent(submissionId, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > MAX_CALLS_PER_SUBMISSION) {
            log.error("Agent 工具调用次数超限: submissionId={}, toolName={}, count={}, max={}",
                    submissionId, toolName, current, MAX_CALLS_PER_SUBMISSION);
            throw new RuntimeException(
                    "工具调用次数超过上限(" + MAX_CALLS_PER_SUBMISSION + ")，疑似模型幻觉死循环，已强制终止。"
            );
        }

        log.debug("Agent 工具调用计数: submissionId={}, toolName={}, count={}", submissionId, toolName, current);
    }

    /**
     * 清理指定 submission 的计数器（评分完成后调用）
     */
    public void cleanup(Long submissionId) {
        if (submissionId != null) {
            counters.remove(submissionId);
        }
    }

    /**
     * 重置指定 submission 的计数器（重试时调用）
     */
    public void reset(Long submissionId) {
        if (submissionId != null) {
            counters.put(submissionId, new AtomicInteger(0));
        }
    }
}
