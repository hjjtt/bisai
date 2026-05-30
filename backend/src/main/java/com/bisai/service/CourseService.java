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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final PermissionService permissionService;

    public Result<PageResult<Course>> listCourses(PageQuery query, Long userId, String role) {
        Page<Course> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        // 教师只能看到自己的课程
        if ("TEACHER".equals(role)) {
            wrapper.eq(Course::getTeacherId, userId);
        }

        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.like(Course::getName, query.getKeyword());
        }
        wrapper.orderByDesc(Course::getCreatedAt);

        Page<Course> result = courseMapper.selectPage(page, wrapper);
        fillTeacherAndClassName(result.getRecords());
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<Course> createCourse(Course course, Long userId, String role) {
        if (permissionService.isAdmin(role)) {
            if (course.getTeacherId() == null) {
                return Result.error(40001, "请选择授课教师");
            }
        } else {
            course.setTeacherId(userId);
        }
        course.setStatus("ENABLED");
        courseMapper.insert(course);
        Course created = courseMapper.selectById(course.getId());
        fillTeacherAndClassName(java.util.List.of(created));
        return Result.ok(created);
    }

    public Result<Course> updateCourse(Long id, Course course, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfCourse(id, userId)) {
            return Result.error(40301, "无权操作该课程");
        }
        course.setId(id);
        courseMapper.updateById(course);
        Course updated = courseMapper.selectById(id);
        fillTeacherAndClassName(java.util.List.of(updated));
        return Result.ok(updated);
    }

    private void fillTeacherAndClassName(java.util.List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }
        Set<Long> teacherIds = courses.stream()
                .map(Course::getTeacherId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> classIds = courses.stream()
                .map(Course::getClassId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> teacherNameMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, teacherIds))
                    .forEach(user -> teacherNameMap.put(user.getId(), user.getRealName()));
        }

        Map<Long, String> classNameMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            classMapper.selectList(new LambdaQueryWrapper<ClassEntity>().in(ClassEntity::getId, classIds))
                    .forEach(classEntity -> classNameMap.put(classEntity.getId(), classEntity.getName()));
        }

        courses.forEach(course -> {
            course.setTeacherName(teacherNameMap.get(course.getTeacherId()));
            course.setClassName(classNameMap.get(course.getClassId()));
        });
    }
}
