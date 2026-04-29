package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.User;
import com.bisai.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public Result<PageResult<User>> listUsers(PageQuery query, String role) {
        Page<User> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, query.getKeyword())
                    .or().like(User::getRealName, query.getKeyword()));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(page, wrapper);
        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));

        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<User> getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }
        user.setPassword(null);
        return Result.ok(user);
    }

    public Result<User> createUser(User user) {
        // 检查用户名唯一
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())
        );
        if (count > 0) {
            return Result.error(40901, "用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("ENABLED");
        userMapper.insert(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    public Result<User> updateUser(Long id, User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            return Result.error(40401, "用户不存在");
        }

        user.setId(id);
        user.setPassword(null); // 不允许通过此接口修改密码
        userMapper.updateById(user);

        User updated = userMapper.selectById(id);
        updated.setPassword(null);
        return Result.ok(updated);
    }

    public Result<Void> resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.ok();
    }

    public Result<Void> toggleStatus(Long id, String status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.ok();
    }
}
