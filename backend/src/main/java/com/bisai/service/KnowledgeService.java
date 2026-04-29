package com.bisai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.KnowledgeBase;
import com.bisai.entity.KnowledgeDocument;
import com.bisai.mapper.KnowledgeBaseMapper;
import com.bisai.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Value("${file.upload-path:./data/files}")
    private String uploadPath;

    public Result<PageResult<KnowledgeDocument>> listDocuments(PageQuery query) {
        Page<KnowledgeDocument> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();

        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.like(KnowledgeDocument::getOriginalName, query.getKeyword());
        }

        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);

        Page<KnowledgeDocument> result = documentMapper.selectPage(page, wrapper);

        result.getRecords().forEach(doc -> {
            doc.setVectorized("SUCCESS".equals(doc.getVectorStatus()));
            doc.setUpdateTime(doc.getUpdatedAt());
            // 设置前端需要的 name 字段（映射自 originalName）
            doc.setName(doc.getOriginalName());
            // 设置关联课程名称
            doc.setCourseName(resolveCourseName(doc));
        });

        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    /**
     * 上传知识库文档（MVP 阶段模拟实现）
     */
    public Result<KnowledgeDocument> uploadDocument(MultipartFile file, Long courseId) {
        try {
            String originalName = file.getOriginalFilename();

            // 保存文件到磁盘
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String storedName = UUID.randomUUID().toString() + ext;
            Path dir = Paths.get(uploadPath, "knowledge");
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            file.transferTo(filePath.toFile());

            // 创建文档记录
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setOriginalName(originalName);
            doc.setFileId(null); // MVP 阶段暂不关联 file 表
            doc.setParseStatus("PENDING");
            doc.setVectorStatus("PENDING");
            doc.setEnabled(true);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());

            // 关联知识库：根据 courseId 查找对应的知识库
            if (courseId != null) {
                KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                        new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getCourseId, courseId)
                );
                if (kb != null) {
                    doc.setKnowledgeBaseId(kb.getId());
                }
            }

            documentMapper.insert(doc);

            // 设置前端需要的字段
            doc.setName(originalName);
            doc.setCourseName(resolveCourseName(doc));

            return Result.ok(doc);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    public Result<Void> deleteDocument(Long id) {
        documentMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 解析文档关联的课程名称
     */
    private String resolveCourseName(KnowledgeDocument doc) {
        if (doc.getKnowledgeBaseId() != null) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
            if (kb != null && kb.getCourseId() != null) {
                return "课程 #" + kb.getCourseId();
            }
        }
        return "知识库 #" + doc.getKnowledgeBaseId();
    }
}
