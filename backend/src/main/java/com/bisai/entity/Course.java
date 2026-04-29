package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long teacherId;
    private Long classId;
    private String description;
    private String status;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String teacherName;
    @TableField(exist = false)
    private String className;
}
