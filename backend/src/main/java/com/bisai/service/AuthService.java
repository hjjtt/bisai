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

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Result<Map<String, Object>> login(LoginRequest request) {
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );

        if (user == null) {
            return Result.error(40101, "用户名或密码错误");
        }

        if ("DISABLED".equals(user.getStatus())) {
            return Result.error(40301, "账号已被禁用");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error(40101, "用户名或密码错误");
        }

        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        user.setPassword(null);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("role", user.getRole());
        userMap.put("realName", user.getRealName());
        userMap.put("classId", user.getClassId());
        userMap.put("status", user.getStatus());
        data.put("user", userMap);

        return Result.ok(data);
    }
}
