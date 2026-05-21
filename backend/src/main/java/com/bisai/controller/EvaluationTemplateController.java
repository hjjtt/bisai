package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.EvaluationTemplate;
import com.bisai.service.EvaluationTemplateService;
import com.bisai.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EvaluationTemplateController {

    private final EvaluationTemplateService templateService;
    private final PermissionService permissionService;

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
    public Result<EvaluationTemplate> create(@RequestBody EvaluationTemplate template, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        template.setCreatorId(userId);
        if (!"ADMIN".equals(role)) {
            template.setStatus(null);
        }
        return templateService.createTemplate(template);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<EvaluationTemplate> update(@PathVariable Long id, @RequestBody EvaluationTemplate template, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role) && !templateService.isOwner(id, userId)) {
            return Result.error(40301, "无权修改该模板");
        }
        template.setCreatorId(null);
        return templateService.updateTemplate(id, template);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role) && !templateService.isOwner(id, userId)) {
            return Result.error(40301, "无权删除该模板");
        }
        return templateService.deleteTemplate(id);
    }
}
