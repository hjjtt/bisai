package com.bisai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.KnowledgeBase;
import com.bisai.entity.KnowledgeDocument;
import com.bisai.entity.FileEntity;
import com.bisai.entity.DocumentChunk;
import com.bisai.entity.ParseResult;
import com.bisai.mapper.DocumentChunkMapper;
import com.bisai.mapper.FileMapper;
import com.bisai.mapper.KnowledgeBaseMapper;
import com.bisai.mapper.KnowledgeDocumentMapper;
import com.bisai.mapper.ParseResultMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class KnowledgeService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final FileMapper fileMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ParseResultMapper parseResultMapper;
    private final DocumentTextExtractor documentTextExtractor;
    private final ModelScopeClient aiClient;
    private final ObjectMapper objectMapper;
    private final Executor aiTaskExecutor;

    @Value("${file.upload-path:./data/files}")
    private String uploadPath;

    public KnowledgeService(KnowledgeDocumentMapper documentMapper,
                            KnowledgeBaseMapper knowledgeBaseMapper,
                            FileMapper fileMapper,
                            DocumentChunkMapper documentChunkMapper,
                            ParseResultMapper parseResultMapper,
                            DocumentTextExtractor documentTextExtractor,
                            ModelScopeClient aiClient,
                            ObjectMapper objectMapper,
                            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.fileMapper = fileMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.parseResultMapper = parseResultMapper;
        this.documentTextExtractor = documentTextExtractor;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.aiTaskExecutor = aiTaskExecutor;
    }

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

            KnowledgeBase kb = resolveKnowledgeBase(courseId);

            // 创建文档记录
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setKnowledgeBaseId(kb.getId());
            doc.setOriginalName(originalName);
            doc.setParseStatus("PENDING");
            doc.setVectorStatus("PENDING");
            doc.setEnabled(true);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());

            documentMapper.insert(doc);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setKnowledgeDocumentId(doc.getId());
            fileEntity.setOriginalName(originalName);
            fileEntity.setFilePath(filePath.toString());
            fileEntity.setFileType(ext.replace(".", "").toUpperCase());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileHash(cn.hutool.crypto.digest.DigestUtil.md5Hex(file.getInputStream()));
            fileMapper.insert(fileEntity);

            doc.setFileId(fileEntity.getId());
            documentMapper.updateById(doc);
            aiTaskExecutor.execute(() -> processKnowledgeDocument(doc.getId()));

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

    public void processKnowledgeDocument(Long documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) return;
        doc.setParseStatus("PARSING");
        doc.setVectorStatus("PENDING");
        documentMapper.updateById(doc);
        try {
            FileEntity file = fileMapper.selectById(doc.getFileId());
            if (file == null) {
                throw new RuntimeException("知识库文档未关联文件");
            }

            DocumentTextExtractor.ExtractedText extracted = documentTextExtractor.extract(file);
            String content = extracted.content();
            if (documentTextExtractor.isImage(file)) {
                String fileType = file.getFileType() == null ? "png" : file.getFileType().toLowerCase();
                content = content + "\n图片多模态分析:\n" + aiClient.analyzeImage(Path.of(file.getFilePath()),
                        "jpg".equals(fileType) ? "image/jpeg" : "image/" + fileType,
                        "请提取这张知识库图片中的课程知识点、评分标准、图表含义和可作为评价参考的信息。");
            }

            String systemPrompt = "你是课程知识库文档解析助手。请将文档内容整理为 JSON，包含 summary、mainTopics、completeness、quality、suggestions 字段。只返回 JSON。";
            JsonNode parsed = parseJson(aiClient.chat(systemPrompt, trim(content, 8000)));
            ParseResult parseResult = new ParseResult();
            parseResult.setKnowledgeDocumentId(documentId);
            parseResult.setFileId(file.getId());
            parseResult.setParserType(extracted.parserType());
            parseResult.setContent(content);
            parseResult.setSummary(parsed.path("summary").asText(""));
            parseResult.setMainTopics(parsed.path("mainTopics").toString());
            parseResult.setCompleteness(parsed.path("completeness").asText(""));
            parseResult.setQuality(parsed.path("quality").asText(""));
            parseResult.setSuggestions(parsed.path("suggestions").toString());
            parseResult.setCreatedAt(LocalDateTime.now());
            parseResultMapper.insert(parseResult);

            doc.setParseStatus("SUCCESS");
            doc.setVectorStatus("VECTORIZING");
            documentMapper.updateById(doc);

            documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKnowledgeDocumentId, documentId));
            List<String> chunks = splitContent(content, 1200);
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                DocumentChunk chunk = new DocumentChunk();
                chunk.setKnowledgeDocumentId(documentId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setTokenCount(Math.max(1, chunkText.length() / 2));
                chunk.setEmbedding(objectMapper.writeValueAsString(aiClient.embedding(chunkText)));
                chunk.setCreatedAt(LocalDateTime.now());
                documentChunkMapper.insert(chunk);
            }

            doc.setVectorStatus("SUCCESS");
            documentMapper.updateById(doc);
        } catch (Exception e) {
            log.error("知识库文档处理失败 documentId={}: {}", documentId, e.getMessage(), e);
            doc.setParseStatus("FAILED");
            doc.setVectorStatus("FAILED");
            documentMapper.updateById(doc);
        }
    }

    private KnowledgeBase resolveKnowledgeBase(Long courseId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(KnowledgeBase::getCourseId, courseId);
        } else {
            wrapper.isNull(KnowledgeBase::getCourseId);
        }
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(wrapper);
        if (kb != null) return kb;

        kb = new KnowledgeBase();
        kb.setName(courseId == null ? "通用知识库" : "课程知识库-" + courseId);
        kb.setCourseId(courseId);
        kb.setStatus("ENABLED");
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    private JsonNode parseJson(String aiResult) throws Exception {
        String json = aiResult;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7, json.lastIndexOf("```"));
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3, json.lastIndexOf("```"));
        }
        return objectMapper.readTree(json.trim());
    }

    private List<String> splitContent(String content, int chunkSize) {
        if (content == null || content.isBlank()) return List.of("");
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        for (int start = 0; start < content.length(); start += chunkSize) {
            int end = Math.min(content.length(), start + chunkSize);
            chunks.add(content.substring(start, end));
        }
        return chunks;
    }

    private String trim(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
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
