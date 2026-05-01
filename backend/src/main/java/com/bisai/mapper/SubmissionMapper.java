package com.bisai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bisai.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("SELECT s.*, u.real_name AS student_name, t.title AS task_title " +
            "FROM submission s " +
            "LEFT JOIN user u ON s.student_id = u.id " +
            "LEFT JOIN training_task t ON s.task_id = t.id " +
            "WHERE s.deleted = 0")
    List<Submission> selectWithDetail();

    @Select("SELECT s.*, u.real_name AS student_name, t.title AS task_title, c.name AS class_name " +
            "FROM submission s " +
            "LEFT JOIN user u ON s.student_id = u.id " +
            "LEFT JOIN training_task t ON s.task_id = t.id " +
            "LEFT JOIN class c ON u.class_id = c.id " +
            "WHERE s.id = #{submissionId} AND s.deleted = 0")
    Submission selectDetailById(@Param("submissionId") Long submissionId);

    @Select("SELECT s.*, u.real_name AS student_name, t.title AS task_title, c.name AS class_name " +
            "FROM submission s " +
            "LEFT JOIN user u ON s.student_id = u.id " +
            "LEFT JOIN training_task t ON s.task_id = t.id " +
            "LEFT JOIN class c ON u.class_id = c.id " +
            "WHERE s.task_id = #{taskId} AND s.deleted = 0 " +
            "ORDER BY s.created_at DESC")
    List<Submission> selectByTaskIdWithDetail(@Param("taskId") Long taskId);
}
