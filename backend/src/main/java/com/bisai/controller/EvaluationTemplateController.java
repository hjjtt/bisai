package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.EvaluationTemplate;
import com.bisai.service.EvaluationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EvaluationTemplateController {

    private final EvaluationTemplateService templateService;

    @GetMapping
    public Result<PageResult<EvaluationTemplate>> list(PageQuery query) {
        return templateService.listTemplates(query);
    }

    @GetMapping("/{id}")
    public Result<EvaluationTemplate> get(@PathVariable Long id) {
        return templateService.getTemplate(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<EvaluationTemplate> create(@RequestBody EvaluationTemplate template) {
        return templateService.createTemplate(template);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<EvaluationTemplate> update(@PathVariable Long id, @RequestBody EvaluationTemplate template) {
        return templateService.updateTemplate(id, template);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        return templateService.deleteTemplate(id);
    }
}
