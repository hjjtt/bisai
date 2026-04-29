package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Submission;
import com.bisai.entity.FileEntity;
import com.bisai.mapper.FileMapper;
import com.bisai.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final FileMapper fileMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    public Result<PageResult<Submission>> listSubmissions(PageQuery query, Long taskId, Long studentId) {
        Page<Submission> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();

        if (taskId != null) {
            wrapper.eq(Submission::getTaskId, taskId);
        }
        if (studentId != null) {
            wrapper.eq(Submission::getStudentId, studentId);
        }
        wrapper.orderByDesc(Submission::getCreatedAt);

        Page<Submission> result = submissionMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<Submission> getSubmission(Long id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        return Result.ok(submission);
    }

    public Result<Void> uploadFiles(Long taskId, Long studentId, MultipartFile[] files) throws IOException {
        // 查询当前版本号
        Long count = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getStudentId, studentId)
        );
        int version = count.intValue() + 1;

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
            String storedName = UUID.randomUUID().toString() + ext;

            Path dir = Paths.get(uploadPath, "submissions", String.valueOf(taskId), String.valueOf(studentId), String.valueOf(version));
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            file.transferTo(filePath.toFile());

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

        return Result.ok();
    }

    public Result<List<FileEntity>> getFileList(Long submissionId) {
        List<FileEntity> files = fileMapper.selectList(
                new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getSubmissionId, submissionId)
        );
        return Result.ok(files);
    }
}
