package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.ClassEntity;
import com.bisai.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public Result<PageResult<ClassEntity>> list(PageQuery query) {
        return classService.listClasses(query);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ClassEntity> create(@RequestBody ClassEntity entity) {
        return classService.createClass(entity);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ClassEntity> update(@PathVariable Long id, @RequestBody ClassEntity entity) {
        return classService.updateClass(id, entity);
    }
}
