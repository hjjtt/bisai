package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Course;
import com.bisai.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<PageResult<Course>> list(PageQuery query) {
        return courseService.listCourses(query);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Course> create(@RequestBody Course course) {
        return courseService.createCourse(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Course> update(@PathVariable Long id, @RequestBody Course course) {
        return courseService.updateCourse(id, course);
    }
}
