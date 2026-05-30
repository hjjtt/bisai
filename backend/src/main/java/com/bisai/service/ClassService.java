package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.ClassEntity;
import com.bisai.entity.User;
import com.bisai.mapper.ClassMapper;
import com.bisai.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;
    private final UserMapper userMapper;

    public Result<PageResult<ClassEntity>> listClasses(PageQuery query) {
        Page<ClassEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();

        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.like(ClassEntity::getName, query.getKeyword());
        }
        wrapper.orderByDesc(ClassEntity::getCreatedAt);

        Page<ClassEntity> result = classMapper.selectPage(page, wrapper);
        fillStudentCount(result.getRecords());
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<ClassEntity> createClass(ClassEntity entity) {
        classMapper.insert(entity);
        return Result.ok(entity);
    }

    public Result<ClassEntity> updateClass(Long id, ClassEntity entity) {
        entity.setId(id);
        classMapper.updateById(entity);
        return Result.ok(classMapper.selectById(id));
    }

    private void fillStudentCount(java.util.List<ClassEntity> classes) {
        if (classes == null || classes.isEmpty()) {
            return;
        }
        Set<Long> classIds = classes.stream()
                .map(ClassEntity::getId)
                .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return;
        }
        Map<Long, Long> studentCountMap = userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getRole, "STUDENT")
                                .in(User::getClassId, classIds)
                ).stream()
                .filter(user -> user.getClassId() != null)
                .collect(Collectors.groupingBy(User::getClassId, Collectors.counting()));

        classes.forEach(classEntity -> classEntity.setStudentCount(
                studentCountMap.getOrDefault(classEntity.getId(), 0L).intValue()
        ));
    }
}
