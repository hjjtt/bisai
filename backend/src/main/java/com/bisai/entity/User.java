package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    // WRITE_ONLY：创建/更新请求可携带密码，但任何响应都不回传（原 @JsonIgnore 导致管理员建用户时密码被丢弃）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String role;
    private String realName;
    private Long classId;
    private String status;
    private Boolean mustChangePassword;
    private LocalDateTime lastPasswordChangeAt;
    private LocalDateTime lastLoginAt;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String teachingClassNames;

    @TableField(exist = false)
    private String teachingCourseNames;

    @TableField(exist = false)
    private String teachingCourseBindings;
}
