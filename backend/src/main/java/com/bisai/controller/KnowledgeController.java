package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.KnowledgeDocument;
import com.bisai.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public Result<PageResult<KnowledgeDocument>> list(PageQuery query) {
        return knowledgeService.listDocuments(query);
    }

    /**
     * 上传知识库文档
     */
    @PostMapping("/upload")
    public Result<KnowledgeDocument> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "courseId", required = false) Long courseId) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        return knowledgeService.uploadDocument(file, courseId);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return knowledgeService.deleteDocument(id);
    }
}
