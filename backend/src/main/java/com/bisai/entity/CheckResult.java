package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_result")
public class CheckResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private String checkType;
    private String checkItem;
    private String result;
    private String description;
    private String evidence;
    private String suggestion;
    private String riskLevel;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
