package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.dto.LoginRequest;
import com.bisai.dto.RegisterRequest;
import com.bisai.entity.User;
import com.bisai.mapper.UserMapper;
import com.bisai.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final com.bisai.mapper.ClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    // 至少 8 位、最长 64 位；字母+数字为基础，允许常用特殊字符；需满足大小写/数字/特殊字符 4 类中至少 3 类
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};:,.?/~]{8,64}$");
    private static final String PASSWORD_RULE_HINT = "密码需 8 位以上，且包含大写字母、小写字母、数字、特殊字符中至少 3 类";

    // 常见弱密码黑名单（小写比对），命中直接拒绝
    private static final java.util.Set<String> WEAK_PASSWORDS = java.util.Set.of(
            "password", "password1", "password123", "passwd123", "admin123", "admin888",
            "root123", "user123", "test123", "guest123", "login123", "qwer1234", "qwerty123",
            "1qaz2wsx", "1q2w3e4r", "qazwsx123", "abcd1234", "abc12345", "abc123456",
            "a1234567", "aa123456", "aaa123456", "12345678", "123456789", "1234567890",
            "12345678a", "123456789a", "11111111", "00000000", "12121212", "11223344",
            "654321ab", "66666666", "88888888", "88888888a", "5201314a", "1314520a",
            "iloveyou1", "letmein123", "welcome123", "monkey123", "dragon123",
            "sunshine1", "princess1", "football1", "baseball1", "shadow123",
            "super123", "master123", "hello123", "freedom123", "whatever1",
            "zaq12wsx", "!qaz2wsx", "p@ssw0rd", "p@ssword123", "trustno1x",
            "abcdefg1", "abcdefg123", "abcdef123", "abcdefgh1", "abcdefghij",
            "qwertyui1", "qwertyuiop", "asdfghjk1", "zxcvbnm12", "1234qwer",
            "1qaz@wsx", "1qaz2wsx3", "qq123456a", "taobao123", "alibaba123",
            "huawei123", "xiaomi123", "baidu1234", "wang123456", "li1234567"
    );

    public Result<Map<String, Object>> login(LoginRequest request) {
        String username = request.getUsername();

        // 检查账号是否被锁定
        if (captchaService.isLocked(username)) {
            long remaining = captchaService.getLockRemainingMinutes(username);
            return Result.error(40103, "账号已被锁定，请 " + remaining + " 分钟后重试");
        }

        // 验证码校验（如果提供了验证码）
        int failureCount = captchaService.getFailureCount(username);
        boolean captchaRequired = failureCount >= 3;
        if (captchaRequired) {
            if (request.getCaptchaUuid() == null || request.getCaptchaCode() == null) {
                return Result.error(40003, "请输入验证码");
            }
            if (!captchaService.verifyCaptcha(request.getCaptchaUuid(), request.getCaptchaCode())) {
                return Result.error(40003, "验证码错误或已过期");
            }
        } else if (request.getCaptchaUuid() != null && request.getCaptchaCode() != null) {
            if (!captchaService.verifyCaptcha(request.getCaptchaUuid(), request.getCaptchaCode())) {
                return Result.error(40003, "验证码错误或已过期");
            }
        }

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            captchaService.recordFailure(username);
            return Result.error(40101, "用户名或密码错误");
        }

        if ("DISABLED".equals(user.getStatus())) {
            return Result.error(40301, "账号已被禁用");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            captchaService.recordFailure(username);
            int remaining = MAX_FAILURES - captchaService.getFailureCount(username);
            if (remaining <= 0) {
                return Result.error(40102, "密码错误次数过多，账号已锁定 " + LOCK_DURATION_MINUTES + " 分钟");
            }
            return Result.error(40101, "用户名或密码错误");
        }

        // 登录成功，清除失败记录
        captchaService.clearFailure(username);

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 检查是否需要修改密码
        boolean mustChangePassword = Boolean.TRUE.equals(user.getMustChangePassword());

        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getLastPasswordChangeAt());

        // 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("mustChangePassword", mustChangePassword);

        user.setPassword(null);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("role", user.getRole());
        userMap.put("realName", user.getRealName());
        userMap.put("classId", user.getClassId());
        userMap.put("status", user.getStatus());
        userMap.put("mustChangePassword", mustChangePassword);
        data.put("user", userMap);

        return Result.ok(data);
    }

    /**
     * 修改密码
     */
    public Result<Void> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(40001, "原密码错误");
        }

        if (!validatePassword(newPassword)) {
            return Result.error(40002, PASSWORD_RULE_HINT);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.ok();
    }

    /**
     * 密码复杂度校验：长度 8-64；字符合法；4 类字符（大写/小写/数字/特殊）至少含 3 类；不得命中常见弱密码黑名单
     */
    public static boolean validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }
        if (WEAK_PASSWORDS.contains(password.toLowerCase(java.util.Locale.ROOT))) {
            return false;
        }
        int classes = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) classes++;
        if (password.chars().anyMatch(Character::isLowerCase)) classes++;
        if (password.chars().anyMatch(Character::isDigit)) classes++;
        if (password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{};:,.?/~".indexOf(c) >= 0)) classes++;
        return classes >= 3;
    }

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    /**
     * 用户注册
     */
    public Result<Void> register(RegisterRequest request) {
        // 注册仅开放学生：角色一律硬编码 STUDENT，忽略客户端传入的 role 字段（防止越权注册教师/管理员）
        final String role = "STUDENT";

        // 学生必须选择班级，且班级必须真实存在（防脏数据：此前未校验存在性，可写入任意 class_id）
        if (request.getClassId() == null) {
            return Result.error(40005, "学生请选择所属班级");
        }
        if (classMapper.selectById(request.getClassId()) == null) {
            return Result.error(40005, "所选班级不存在");
        }

        // 用户名唯一性
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existing != null) {
            return Result.error(40006, "用户名已存在");
        }

        // 密码复杂度
        if (!validatePassword(request.getPassword())) {
            return Result.error(40002, PASSWORD_RULE_HINT);
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(role);
        user.setClassId(request.getClassId());
        user.setStatus("ENABLED");
        user.setMustChangePassword(false);
        userMapper.insert(user);

        return Result.ok();
    }
}
