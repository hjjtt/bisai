package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.dto.LoginRequest;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    public Result<Map<String, Object>> login(LoginRequest request) {
        String username = request.getUsername();

        // 检查账号是否被锁定
        if (captchaService.isLocked(username)) {
            long remaining = captchaService.getLockRemainingMinutes(username);
            return Result.error(40103, "账号已被锁定，请 " + remaining + " 分钟后重试");
        }

        // 验证码校验（如果提供了验证码）
        if (request.getCaptchaUuid() != null && request.getCaptchaCode() != null) {
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
            return Result.error(40101, "用户名或密码错误（剩余" + remaining + "次机会）");
        }

        // 登录成功，清除失败记录
        captchaService.clearFailure(username);

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 检查是否需要修改密码
        boolean mustChangePassword = Boolean.TRUE.equals(user.getMustChangePassword());

        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

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
            return Result.error(40002, "密码必须包含字母和数字，且长度至少8位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.ok();
    }

    /**
     * 密码复杂度校验
     */
    public static boolean validatePassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
}
