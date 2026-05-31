package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.dto.LoginRequest;
import com.bisai.dto.RegisterRequest;
import com.bisai.entity.ClassEntity;
import com.bisai.mapper.ClassMapper;
import com.bisai.service.AuthService;
import com.bisai.service.CaptchaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final ClassMapper classMapper;

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return Result.ok(captchaService.generateCaptcha());
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/classes")
    public Result<List<ClassEntity>> listClassesForRegister() {
        List<ClassEntity> classes = classMapper.selectList(
                new LambdaQueryWrapper<ClassEntity>().eq(ClassEntity::getStatus, "ENABLED")
        );
        return Result.ok(classes);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return authService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
    }
}
