package com.bisai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bisai.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
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
}
