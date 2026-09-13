package com.bisai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度超出限制")
    private String username;
    @NotBlank(message = "密码不能为空")
    // BCrypt 有效输入仅 72 字节；登录前先截断超长输入，防止对超长密码执行哈希烧 CPU（破坏性测试发现的枯竭向量）
    @Size(max = 128, message = "密码长度超出限制")
    private String password;
    private String captchaUuid;
    private String captchaCode;
}
