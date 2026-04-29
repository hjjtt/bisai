package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("indicator")
public class Indicator {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long parentId;
    private String name;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private String scoreRule;
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<Indicator> children;
}
