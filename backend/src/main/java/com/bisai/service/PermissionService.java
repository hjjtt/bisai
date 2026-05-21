package com.bisai.service;

import com.bisai.entity.*;
import com.bisai.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final CourseMapper courseMapper;
    private final TrainingTaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;

    public boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    public boolean isTeacherOwnerOfCourse(Long courseId, Long userId) {
        if (courseId == null || userId == null) return false;
        Course course = courseMapper.selectById(courseId);
        return course != null && userId.equals(course.getTeacherId());
    }

    public boolean isTeacherOwnerOfTask(Long taskId, Long userId) {
        if (taskId == null || userId == null) return false;
        TrainingTask task = taskMapper.selectById(taskId);
        return task != null && isTeacherOwnerOfCourse(task.getCourseId(), userId);
    }

    public boolean isTeacherOwnerOfSubmission(Long submissionId, Long userId) {
        if (submissionId == null || userId == null) return false;
        Submission sub = submissionMapper.selectById(submissionId);
        return sub != null && isTeacherOwnerOfTask(sub.getTaskId(), userId);
    }

    public Long getCourseIdByTask(Long taskId) {
        if (taskId == null) return null;
        TrainingTask task = taskMapper.selectById(taskId);
        return task != null ? task.getCourseId() : null;
    }

    public Long getCourseIdBySubmission(Long submissionId) {
        if (submissionId == null) return null;
        Submission sub = submissionMapper.selectById(submissionId);
        return sub != null ? getCourseIdByTask(sub.getTaskId()) : null;
    }
}
