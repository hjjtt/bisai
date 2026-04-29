package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Course;
import com.bisai.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;

    public Result<PageResult<Course>> listCourses(PageQuery query) {
        Page<Course> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.like(Course::getName, query.getKeyword());
        }
        wrapper.orderByDesc(Course::getCreatedAt);

        Page<Course> result = courseMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<Course> createCourse(Course course) {
        course.setStatus("ENABLED");
        courseMapper.insert(course);
        return Result.ok(course);
    }

    public Result<Course> updateCourse(Long id, Course course) {
        course.setId(id);
        courseMapper.updateById(course);
        return Result.ok(courseMapper.selectById(id));
    }
}
