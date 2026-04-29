package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String model;
    private String callType;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
}
