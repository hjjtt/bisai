package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.ClassEntity;
import com.bisai.mapper.ClassMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;

    public Result<PageResult<ClassEntity>> listClasses(PageQuery query) {
        Page<ClassEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();

        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.like(ClassEntity::getName, query.getKeyword());
        }
        wrapper.orderByDesc(ClassEntity::getCreatedAt);

        Page<ClassEntity> result = classMapper.selectPage(page, wrapper);
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
}
