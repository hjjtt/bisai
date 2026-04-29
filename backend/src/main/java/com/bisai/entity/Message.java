package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private Long relatedId;
    private LocalDateTime createdAt;
}
