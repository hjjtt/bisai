package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.KnowledgeDocument;
import com.bisai.service.KnowledgeService;
import com.bisai.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<PageResult<KnowledgeDocument>> list(PageQuery query) {
        return knowledgeService.listDocuments(query);
    }

    /**
     * 上传知识库文档
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<KnowledgeDocument> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "courseId", required = false) Long courseId,
                                             @RequestParam(value = "taskId", required = false) Long taskId,
                                             Authentication auth) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role)) {
            if (taskId != null && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
                return Result.error(40301, "无权为该实训任务上传知识库文档");
            }
            if (taskId == null && courseId != null && !permissionService.isTeacherOwnerOfCourse(courseId, userId)) {
                return Result.error(40301, "无权为该课程上传知识库文档");
            }
            if (taskId == null && courseId == null) {
                return Result.error(40001, "请选择关联实训任务");
            }
        }
        return knowledgeService.uploadDocument(file, courseId, taskId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role) && !knowledgeService.isOwner(id, userId)) {
            return Result.error(40301, "无权删除该文档");
        }
        return knowledgeService.deleteDocument(id);
    }

    /**
     * 切换文档启用状态
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role) && !knowledgeService.isOwner(id, userId)) {
            return Result.error(40301, "无权操作该文档");
        }
        Boolean enabled = body.get("enabled");
        return knowledgeService.toggleDocumentStatus(id, enabled);
    }
}
