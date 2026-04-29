package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class")
public class ClassEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String grade;
    private String major;
    private String status;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
