package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.ClassEntity;
import com.bisai.entity.Course;
import com.bisai.entity.User;
import com.bisai.mapper.ClassMapper;
import com.bisai.mapper.CourseMapper;
import com.bisai.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;
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
        fillClassInfo(result.getRecords());
        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));

        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<User> getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }
        fillClassInfo(List.of(user));
        user.setPassword(null);
        return Result.ok(user);
    }

    public Result<User> createUser(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return Result.error(40001, "用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return Result.error(40001, "密码不能为空");
        }
        if (!isValidRole(user.getRole())) {
            return Result.error(40001, "无效的角色");
        }
        if ("STUDENT".equals(user.getRole())) {
            if (user.getClassId() == null) {
                return Result.error(40001, "学生必须绑定班级");
            }
            if (classMapper.selectById(user.getClassId()) == null) {
                return Result.error(40401, "班级不存在");
            }
        } else {
            user.setClassId(null);
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())
        );
        if (count > 0) {
            return Result.error(40901, "用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("ENABLED");
        user.setId(null);
        user.setDeleted(null);
        userMapper.insert(user);
        fillClassInfo(List.of(user));
        user.setPassword(null);
        return Result.ok(user);
    }

    public Result<User> updateUser(Long id, User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            return Result.error(40401, "用户不存在");
        }
        if (user.getRealName() == null || user.getRealName().isBlank()) {
            return Result.error(40001, "姓名不能为空");
        }

        String targetRole = user.getRole() != null ? user.getRole() : existing.getRole();
        if (!isValidRole(targetRole)) {
            return Result.error(40001, "无效的角色");
        }

        Long targetClassId;
        if ("STUDENT".equals(targetRole)) {
            targetClassId = user.getClassId() != null ? user.getClassId() : existing.getClassId();
            if (targetClassId == null) {
                return Result.error(40001, "学生必须绑定班级");
            }
            if (classMapper.selectById(targetClassId) == null) {
                return Result.error(40401, "班级不存在");
            }
        } else {
            targetClassId = null;
        }

        if (targetClassId == null) {
            UpdateWrapper<User> wrapper = new UpdateWrapper<User>()
                    .eq("id", id)
                    .set("real_name", user.getRealName())
                    .set("role", targetRole)
                    .set("class_id", null);
            userMapper.update(null, wrapper);
        } else {
            User updateEntity = new User();
            updateEntity.setId(id);
            updateEntity.setRealName(user.getRealName());
            updateEntity.setRole(targetRole);
            updateEntity.setClassId(targetClassId);
            updateEntity.setPassword(null);
            updateEntity.setStatus(null);
            updateEntity.setDeleted(null);
            userMapper.updateById(updateEntity);
        }

        User updated = userMapper.selectById(id);
        fillClassInfo(List.of(updated));
        updated.setPassword(null);
        return Result.ok(updated);
    }

    public Result<Void> resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        user.setMustChangePassword(true);
        userMapper.updateById(user);
        return Result.ok();
    }

    public Result<Void> toggleStatus(Long id, String status) {
        if (!java.util.List.of("ENABLED", "DISABLED").contains(status)) {
            return Result.error(40001, "无效的状态值");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(40401, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.ok();
    }

    private boolean isValidRole(String role) {
        return "STUDENT".equals(role) || "TEACHER".equals(role) || "ADMIN".equals(role);
    }

    private void fillClassInfo(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> studentClassIds = users.stream()
                .filter(user -> "STUDENT".equals(user.getRole()))
                .map(User::getClassId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> teacherIds = users.stream()
                .filter(user -> "TEACHER".equals(user.getRole()))
                .map(User::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<Course> teacherCourses = List.of();
        Set<Long> teacherClassIds = Set.of();
        if (!teacherIds.isEmpty()) {
            teacherCourses = courseMapper.selectList(
                    new LambdaQueryWrapper<Course>().in(Course::getTeacherId, teacherIds)
            );
            teacherClassIds = teacherCourses.stream()
                    .map(Course::getClassId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        Set<Long> classIds = new java.util.HashSet<>(studentClassIds);
        classIds.addAll(teacherClassIds);
        Map<Long, String> classNameMap = Map.of();
        if (!classIds.isEmpty()) {
            classNameMap = classMapper.selectList(
                            new LambdaQueryWrapper<ClassEntity>().in(ClassEntity::getId, classIds)
                    ).stream()
                    .collect(Collectors.toMap(ClassEntity::getId, ClassEntity::getName, (a, b) -> a));
        }
        Map<Long, String> finalClassNameMap = classNameMap;

        Map<Long, LinkedHashSet<String>> teacherClassNameMap = new HashMap<>();
        Map<Long, LinkedHashSet<String>> teacherCourseNameMap = new HashMap<>();
        Map<Long, LinkedHashSet<String>> teacherCourseBindingMap = new HashMap<>();
        teacherCourses.forEach(course -> {
            if (course.getTeacherId() == null) {
                return;
            }
            String courseName = course.getName();
            if (courseName == null || courseName.isBlank()) {
                return;
            }
            teacherCourseNameMap
                    .computeIfAbsent(course.getTeacherId(), key -> new LinkedHashSet<>())
                    .add(courseName);
            String className = course.getClassId() == null ? null : finalClassNameMap.get(course.getClassId());
            if (className != null && !className.isBlank()) {
                teacherClassNameMap
                        .computeIfAbsent(course.getTeacherId(), key -> new LinkedHashSet<>())
                        .add(className);
            }
            String binding = (className == null || className.isBlank())
                    ? courseName
                    : courseName + "(" + className + ")";
            teacherCourseBindingMap
                    .computeIfAbsent(course.getTeacherId(), key -> new LinkedHashSet<>())
                    .add(binding);
        });

        users.forEach(user -> {
            user.setClassName(null);
            user.setTeachingClassNames(null);
            user.setTeachingCourseNames(null);
            user.setTeachingCourseBindings(null);
            if ("STUDENT".equals(user.getRole()) && user.getClassId() != null) {
                user.setClassName(finalClassNameMap.get(user.getClassId()));
            }
            if ("TEACHER".equals(user.getRole()) && user.getId() != null) {
                LinkedHashSet<String> classNames = teacherClassNameMap.get(user.getId());
                if (classNames != null && !classNames.isEmpty()) {
                    user.setTeachingClassNames(String.join(", ", classNames));
                }
                LinkedHashSet<String> courseNames = teacherCourseNameMap.get(user.getId());
                if (courseNames != null && !courseNames.isEmpty()) {
                    user.setTeachingCourseNames(String.join(", ", courseNames));
                }
                LinkedHashSet<String> bindings = teacherCourseBindingMap.get(user.getId());
                if (bindings != null && !bindings.isEmpty()) {
                    user.setTeachingCourseBindings(String.join(", ", bindings));
                }
            }
        });
    }
}
