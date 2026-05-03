package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Course;
import com.bisai.entity.Submission;
import com.bisai.entity.FileEntity;
import com.bisai.entity.TrainingTask;
import com.bisai.entity.User;
import com.bisai.mapper.CourseMapper;
import com.bisai.mapper.FileMapper;
import com.bisai.mapper.SubmissionMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.bisai.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final FileMapper fileMapper;
    private final TrainingTaskMapper taskMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.max-versions:5}")
    private int maxVersions;

    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            "DOC", "DOCX", "PDF", "JPG", "JPEG", "PNG", "XLS", "XLSX", "ZIP"
    );

    public Result<PageResult<Submission>> listSubmissions(PageQuery query, Long taskId, Long studentId, Long userId, String role) {
        Page<Submission> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();

        // 数据权限隔离
        if ("STUDENT".equals(role)) {
            // 学生只能查看自己的提交
            wrapper.eq(Submission::getStudentId, userId);
        } else if ("TEACHER".equals(role)) {
            // 教师只能查看自己课程下的提交
            if (taskId != null) {
                wrapper.eq(Submission::getTaskId, taskId);
            } else {
                // 查询该教师所有课程下的任务提交
                List<Long> taskIds = taskMapper.selectList(
                        new LambdaQueryWrapper<TrainingTask>()
                                .exists("SELECT 1 FROM course c WHERE c.id = training_task.course_id AND c.teacher_id = {0}", userId)
                ).stream().map(TrainingTask::getId).toList();
                if (taskIds.isEmpty()) {
                    return Result.ok(new PageResult<>(List.of(), page.getCurrent(), page.getSize(), 0));
                }
                wrapper.in(Submission::getTaskId, taskIds);
            }
        }
        // 管理员不添加额外过滤

        if (taskId != null && !"TEACHER".equals(role)) {
            wrapper.eq(Submission::getTaskId, taskId);
        }
        if (studentId != null && !"STUDENT".equals(role)) {
            wrapper.eq(Submission::getStudentId, studentId);
        }
        wrapper.orderByDesc(Submission::getCreatedAt);

        Page<Submission> result = submissionMapper.selectPage(page, wrapper);

        // 批量填充任务标题和学生姓名
        if (!result.getRecords().isEmpty()) {
            Set<Long> taskIds = result.getRecords().stream()
                    .map(Submission::getTaskId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            Set<Long> studentIds = result.getRecords().stream()
                    .map(Submission::getStudentId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.Map<Long, String> taskTitleMap = new java.util.HashMap<>();
            if (!taskIds.isEmpty()) {
                taskMapper.selectList(new LambdaQueryWrapper<TrainingTask>().in(TrainingTask::getId, taskIds)).forEach(t -> taskTitleMap.put(t.getId(), t.getTitle()));
            }

            java.util.Map<Long, String> studentNameMap = new java.util.HashMap<>();
            if (!studentIds.isEmpty()) {
                userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, studentIds))
                        .forEach(u -> studentNameMap.put(u.getId(), u.getRealName()));
            }

            result.getRecords().forEach(sub -> {
                sub.setTaskTitle(taskTitleMap.get(sub.getTaskId()));
                sub.setStudentName(studentNameMap.get(sub.getStudentId()));
            });
        }

        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<Submission> getSubmission(Long id, Long userId, String role) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // 填充学生姓名和任务标题
        User student = userMapper.selectById(submission.getStudentId());
        if (student != null) {
            submission.setStudentName(student.getRealName());
        }
        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        if (task != null) {
            submission.setTaskTitle(task.getTitle());
        }

        // 数据权限校验
        if ("STUDENT".equals(role) && !submission.getStudentId().equals(userId)) {
            return Result.error(40301, "无权查看此提交");
        }
        if ("TEACHER".equals(role)) {
            Course course = courseMapper.selectOne(
                    new LambdaQueryWrapper<Course>()
                            .exists("SELECT 1 FROM training_task tt WHERE tt.course_id = course.id AND tt.id = {0}", submission.getTaskId())
                            .eq(Course::getTeacherId, userId)
            );
            if (course == null) {
                return Result.error(40301, "无权查看此提交");
            }
        }
        return Result.ok(submission);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> uploadFiles(Long taskId, Long studentId, MultipartFile[] files) throws IOException {
        // 通过数据库 MAX 查询确定当前最大版本号，避免并发冲突
        List<Submission> existing = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getStudentId, studentId)
                        .select(Submission::getVersion)
        );
        int version = existing.stream()
                .map(Submission::getVersion)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        long count = existing.size();

        // 检查版本数量限制
        if (count >= maxVersions) {
            // 删除最早的提交记录及其关联文件
            List<Submission> oldSubmissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getTaskId, taskId)
                            .eq(Submission::getStudentId, studentId)
                            .orderByAsc(Submission::getVersion)
                            .last("LIMIT " + ((int) count - maxVersions + 1))
            );
            for (Submission old : oldSubmissions) {
                // 逻辑删除旧提交关联的文件
                fileMapper.delete(new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getSubmissionId, old.getId()));
                // 逻辑删除旧提交
                submissionMapper.deleteById(old.getId());
            }
        }

        // 创建提交记录
        Submission submission = new Submission();
        submission.setTaskId(taskId);
        submission.setStudentId(studentId);
        submission.setVersion(version);
        submission.setParseStatus("PENDING");
        submission.setCheckStatus("NOT_CHECKED");
        submission.setScoreStatus("NOT_SCORED");
        submission.setSubmitTime(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 保存文件
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String extUpper = ext.replace(".", "").toUpperCase();

            // 校验文件扩展名
            if (extUpper.isEmpty() || !ALLOWED_EXTENSIONS.contains(extUpper)) {
                return Result.error(40001, "不支持的文件类型: " + ext);
            }

            // 校验 MIME 类型
            String contentType = file.getContentType();
            if (contentType != null && !isValidMimeType(contentType, extUpper)) {
                log.warn("文件MIME类型不匹配: originalName={}, contentType={}, ext={}", originalName, contentType, ext);
            }

            String storedName = UUID.randomUUID().toString() + ext;

            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path dir = baseDir.resolve("submissions")
                    .resolve(String.valueOf(taskId))
                    .resolve(String.valueOf(studentId))
                    .resolve(String.valueOf(version));
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setSubmissionId(submission.getId());
            fileEntity.setOriginalName(originalName);
            fileEntity.setFilePath(filePath.toString());
            fileEntity.setFileType(ext.replace(".", "").toUpperCase());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileHash(cn.hutool.crypto.digest.DigestUtil.md5Hex(file.getInputStream()));
            fileEntity.setVersion(version);
            fileMapper.insert(fileEntity);
        }

        // 发送消息通知教师
        try {
            TrainingTask task = taskMapper.selectById(taskId);
            if (task != null) {
                Course course = courseMapper.selectById(task.getCourseId());
                if (course != null && course.getTeacherId() != null) {
                    String courseName = course.getName() != null ? course.getName() : "";
                    messageService.sendMessage(
                            course.getTeacherId(),
                            "SUBMISSION",
                            "学生提交实训成果",
                            String.format("学生（ID:%d）已提交任务「%s」的成果文件（课程：%s，版本：%d），请及时处理。",
                                    studentId, task.getTitle(), courseName, version),
                            submission.getId()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("发送提交通知消息失败: {}", e.getMessage());
        }

        return Result.ok();
    }

    public Result<List<FileEntity>> getFileList(Long submissionId) {
        List<FileEntity> files = fileMapper.selectList(
                new LambdaQueryWrapper<FileEntity>()
                        .eq(FileEntity::getSubmissionId, submissionId)
                        .notLike(FileEntity::getOriginalName, "学生报告_%")
                        .notLike(FileEntity::getOriginalName, "班级报表_%")
                        .orderByAsc(FileEntity::getCreatedAt)
        );
        return Result.ok(files);
    }

    private boolean isValidMimeType(String contentType, String ext) {
        java.util.Map<String, java.util.List<String>> mimeMap = java.util.Map.of(
                "PDF", java.util.List.of("application/pdf"),
                "DOC", java.util.List.of("application/msword"),
                "DOCX", java.util.List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                "XLS", java.util.List.of("application/vnd.ms-excel"),
                "XLSX", java.util.List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                "JPG", java.util.List.of("image/jpeg"),
                "JPEG", java.util.List.of("image/jpeg"),
                "PNG", java.util.List.of("image/png"),
                "ZIP", java.util.List.of("application/zip", "application/x-zip-compressed")
        );
        java.util.List<String> valid = mimeMap.get(ext);
        return valid != null && valid.stream().anyMatch(contentType::startsWith);
    }
}
