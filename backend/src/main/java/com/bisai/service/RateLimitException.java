package com.bisai.service;

/**
 * AI 调用配额超限异常（HTTP 429）
 * 与普通异常区分，用于友好提示而非报错
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
