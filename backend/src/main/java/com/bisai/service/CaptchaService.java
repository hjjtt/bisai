package com.bisai.service;

import com.bisai.util.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CaptchaService {

    // 验证码存储 (key: uuid, value: {code, expireTime})
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    // 登录失败记录 (key: username, value: {count, lockUntil})
    private final Map<String, LoginFailureEntry> failureStore = new ConcurrentHashMap<>();

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private static final int CAPTCHA_EXPIRE_SECONDS = 120;

    /**
     * 生成验证码
     */
    public Map<String, String> generateCaptcha() {
        CaptchaUtil.CaptchaResult result = CaptchaUtil.generate();
        String uuid = java.util.UUID.randomUUID().toString();
        captchaStore.put(uuid, new CaptchaEntry(result.code(), LocalDateTime.now().plusSeconds(CAPTCHA_EXPIRE_SECONDS)));

        // 清理过期验证码
        cleanupExpiredCaptchas();

        return Map.of("uuid", uuid, "image", result.imageBase64());
    }

    /**
     * 验证验证码
     */
    public boolean verifyCaptcha(String uuid, String code) {
        if (uuid == null || code == null) return false;
        CaptchaEntry entry = captchaStore.remove(uuid);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expireTime())) return false;
        return entry.code().equalsIgnoreCase(code.trim());
    }

    /**
     * 检查账号是否被锁定
     */
    public boolean isLocked(String username) {
        LoginFailureEntry entry = failureStore.get(username);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.lockUntil)) {
            failureStore.remove(username);
            return false;
        }
        return true;
    }

    /**
     * 获取锁定剩余时间（分钟）
     */
    public long getLockRemainingMinutes(String username) {
        LoginFailureEntry entry = failureStore.get(username);
        if (entry == null) return 0;
        long minutes = java.time.Duration.between(LocalDateTime.now(), entry.lockUntil).toMinutes();
        return Math.max(0, minutes);
    }

    /**
     * 记录登录失败
     */
    public void recordFailure(String username) {
        LoginFailureEntry entry = failureStore.computeIfAbsent(username,
                k -> new LoginFailureEntry(0, null));
        entry.failCount++;

        if (entry.failCount >= MAX_FAILURES) {
            entry.lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            log.warn("用户 {} 登录失败次数过多，账号锁定 {} 分钟", username, LOCK_DURATION_MINUTES);
        }
    }

    /**
     * 登录成功，清除失败记录
     */
    public void clearFailure(String username) {
        failureStore.remove(username);
    }

    /**
     * 获取失败次数
     */
    public int getFailureCount(String username) {
        LoginFailureEntry entry = failureStore.get(username);
        return entry != null ? entry.failCount : 0;
    }

    private void cleanupExpiredCaptchas() {
        LocalDateTime now = LocalDateTime.now();
        captchaStore.entrySet().removeIf(e -> now.isAfter(e.getValue().expireTime()));
    }

    private record CaptchaEntry(String code, LocalDateTime expireTime) {}

    private static class LoginFailureEntry {
        int failCount;
        LocalDateTime lockUntil;

        LoginFailureEntry(int failCount, LocalDateTime lockUntil) {
            this.failCount = failCount;
            this.lockUntil = lockUntil;
        }
    }
}
