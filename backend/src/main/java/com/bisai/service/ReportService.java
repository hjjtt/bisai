package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.entity.Submission;
import com.bisai.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SubmissionMapper submissionMapper;

    /**
     * 导出学生个人报告（MVP 阶段模拟实现）
     */
    public Result<Map<String, Object>> exportStudentReport(Long submissionId, String format) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // MVP 阶段：返回模拟数据
        Map<String, Object> data = new HashMap<>();
        data.put("fileId", submissionId);
        data.put("fileName", "学生报告_" + submissionId + "." + format.toLowerCase());
        return Result.ok(data);
    }

    /**
     * 导出班级报告（MVP 阶段模拟实现）
     */
    public Result<Map<String, Object>> exportClassReport(Long taskId, String format) {
        // MVP 阶段：返回模拟数据
        Map<String, Object> data = new HashMap<>();
        data.put("fileId", taskId);
        data.put("fileName", "班级报告_" + taskId + "." + format.toLowerCase());
        return Result.ok(data);
    }
}
