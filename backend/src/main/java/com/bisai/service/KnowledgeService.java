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
import com.bisai.entity.Course;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.DocumentChunkMapper;
import com.bisai.mapper.FileMapper;
import com.bisai.mapper.KnowledgeBaseMapper;
import com.bisai.mapper.KnowledgeDocumentMapper;
import com.bisai.mapper.ParseResultMapper;
import com.bisai.mapper.CourseMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final CourseMapper courseMapper;
    private final TrainingTaskMapper taskMapper;

    @Value("${file.upload-path:./data/files}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "DOC", "DOCX", "PDF", "TXT", "MD", "JPG", "JPEG", "PNG", "XLS", "XLSX", "PPT", "PPTX"
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public KnowledgeService(KnowledgeDocumentMapper documentMapper,
                            KnowledgeBaseMapper knowledgeBaseMapper,
                            FileMapper fileMapper,
                            DocumentChunkMapper documentChunkMapper,
                            ParseResultMapper parseResultMapper,
                            DocumentTextExtractor documentTextExtractor,
                            ModelScopeClient aiClient,
                            ObjectMapper objectMapper,
                            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor,
                            CourseMapper courseMapper,
                            TrainingTaskMapper taskMapper) {
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.fileMapper = fileMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.parseResultMapper = parseResultMapper;
        this.documentTextExtractor = documentTextExtractor;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.aiTaskExecutor = aiTaskExecutor;
        this.courseMapper = courseMapper;
        this.taskMapper = taskMapper;
    }

    public boolean isOwner(Long documentId, Long userId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null || doc.getKnowledgeBaseId() == null) return false;
        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
        Long courseId = kb.getCourseId();
        if (courseId == null && kb.getTaskId() != null) {
            TrainingTask task = taskMapper.selectById(kb.getTaskId());
            courseId = task != null ? task.getCourseId() : null;
        }
        if (courseId == null) return false;
        Course course = courseMapper.selectById(courseId);
        return course != null && userId.equals(course.getTeacherId());
    }

    public Result<PageResult<KnowledgeDocument>> listDocuments(PageQuery query) {
        Page<KnowledgeDocument> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();

        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.like(KnowledgeDocument::getOriginalName, query.getKeyword());
        }

        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);

        Page<KnowledgeDocument> result = documentMapper.selectPage(page, wrapper);

        // 批量查询知识库名称，避免N+1
        Set<Long> kbIds = result.getRecords().stream()
                .map(KnowledgeDocument::getKnowledgeBaseId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, KnowledgeBase> kbMap = new java.util.HashMap<>();
        if (!kbIds.isEmpty()) {
            knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>().in(KnowledgeBase::getId, kbIds))
                    .forEach(kb -> kbMap.put(kb.getId(), kb));
        }

        Set<Long> courseIds = kbMap.values().stream()
                .map(KnowledgeBase::getCourseId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, Course> courseMap = new java.util.HashMap<>();
        if (!courseIds.isEmpty()) {
            courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds))
                    .forEach(course -> courseMap.put(course.getId(), course));
        }

        Set<Long> taskIds = kbMap.values().stream()
                .map(KnowledgeBase::getTaskId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, TrainingTask> taskMap = new java.util.HashMap<>();
        if (!taskIds.isEmpty()) {
            taskMapper.selectList(new LambdaQueryWrapper<TrainingTask>().in(TrainingTask::getId, taskIds))
                    .forEach(task -> taskMap.put(task.getId(), task));
        }

        result.getRecords().forEach(doc -> {
            doc.setVectorized("SUCCESS".equals(doc.getVectorStatus()));
            doc.setUpdateTime(doc.getUpdatedAt());
            doc.setName(doc.getOriginalName());
            KnowledgeBase kb = kbMap.get(doc.getKnowledgeBaseId());
            fillDisplayFields(doc, kb, courseMap, taskMap);
        });

        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    /**
     * 上传知识库文档（MVP 阶段模拟实现）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<KnowledgeDocument> uploadDocument(MultipartFile file, Long courseId, Long taskId) {
        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                return Result.error("文件名不能为空");
            }

            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".") + 1).toUpperCase()
                    : "";
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                return Result.error("不支持的文件类型，允许: " + String.join(", ", ALLOWED_EXTENSIONS));
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return Result.error("文件大小不能超过 50MB");
            }

            KnowledgeBase kb = resolveKnowledgeBase(courseId, taskId);

            String storedExt = originalName.substring(originalName.lastIndexOf("."));
            String storedName = UUID.randomUUID().toString() + storedExt;
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path dir = baseDir.resolve("knowledge");
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

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
            fileEntity.setFileType(ext);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileHash(cn.hutool.crypto.digest.DigestUtil.md5Hex(file.getInputStream()));
            fileMapper.insert(fileEntity);

            doc.setFileId(fileEntity.getId());
            documentMapper.updateById(doc);
            aiTaskExecutor.execute(() -> processKnowledgeDocument(doc.getId()));

            // 设置前端需要的字段
            doc.setName(originalName);
            fillDisplayFields(doc, kb, Map.of(), Map.of());

            return Result.ok(doc);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    public Result<Void> deleteDocument(Long id) {
        documentMapper.deleteById(id);
        return Result.ok();
    }

    public Result<Void> toggleDocumentStatus(Long id, Boolean enabled) {
        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }
        doc.setEnabled(enabled != null && enabled);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
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

    private KnowledgeBase resolveKnowledgeBase(Long courseId, Long taskId) {
        TrainingTask task = null;
        Long resolvedCourseId = courseId;
        if (taskId != null) {
            task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("实训任务不存在");
            }
            resolvedCourseId = task.getCourseId();
        }

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (resolvedCourseId != null) {
            wrapper.eq(KnowledgeBase::getCourseId, resolvedCourseId);
        } else {
            wrapper.isNull(KnowledgeBase::getCourseId);
        }
        if (taskId != null) {
            wrapper.eq(KnowledgeBase::getTaskId, taskId);
        } else {
            wrapper.isNull(KnowledgeBase::getTaskId);
        }
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(wrapper);
        if (kb != null) return kb;

        kb = new KnowledgeBase();
        if (task != null) {
            kb.setName("任务知识库-" + task.getTitle());
        } else {
            kb.setName(resolvedCourseId == null ? "通用知识库" : "课程知识库-" + resolvedCourseId);
        }
        kb.setCourseId(resolvedCourseId);
        kb.setTaskId(taskId);
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
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            if (end < content.length()) {
                // 在 chunkSize 范围内找最近的段落/句子边界
                int boundary = findSplitBoundary(content, start, end, chunkSize);
                if (boundary > start) {
                    end = boundary;
                }
            }
            chunks.add(content.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private int findSplitBoundary(String content, int start, int preferredEnd, int chunkSize) {
        // 优先在段落边界（\n\n）分割
        for (int i = preferredEnd; i > start + chunkSize / 2; i--) {
            if (i < content.length() && content.charAt(i) == '\n'
                    && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                return i;
            }
        }
        // 其次在换行符分割
        for (int i = preferredEnd; i > start + chunkSize / 2; i--) {
            if (i < content.length() && content.charAt(i) == '\n') {
                return i;
            }
        }
        // 最后在句号/感叹号/问号分割
        for (int i = preferredEnd; i > start + chunkSize / 2; i--) {
            if (i < content.length()) {
                char c = content.charAt(i);
                if (c == '。' || c == '.' || c == '！' || c == '？' || c == '!' || c == '?') {
                    return i + 1;
                }
            }
        }
        return preferredEnd;
    }

    private String trim(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private void fillDisplayFields(KnowledgeDocument doc,
                                   KnowledgeBase kb,
                                   Map<Long, Course> courseMap,
                                   Map<Long, TrainingTask> taskMap) {
        if (kb == null) {
            doc.setCourseName("知识库 #" + doc.getKnowledgeBaseId());
            return;
        }

        doc.setTaskId(kb.getTaskId());

        if (kb.getCourseId() != null) {
            Course course = courseMap.get(kb.getCourseId());
            if (course == null) {
                course = courseMapper.selectById(kb.getCourseId());
            }
            doc.setCourseName(course != null ? course.getName() : "课程 #" + kb.getCourseId());
        } else {
            doc.setCourseName("通用资料");
        }

        if (kb.getTaskId() != null) {
            TrainingTask task = taskMap.get(kb.getTaskId());
            if (task == null) {
                task = taskMapper.selectById(kb.getTaskId());
            }
            doc.setTaskName(task != null ? task.getTitle() : "任务 #" + kb.getTaskId());
        } else {
            doc.setTaskName("课程通用知识库");
        }
    }
}
