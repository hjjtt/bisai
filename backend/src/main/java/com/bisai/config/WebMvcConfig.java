package com.bisai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 文件下载已由 FileController 统一处理（带权限校验），
    // 不再通过静态资源映射暴露 /files/** 路径，避免绕过认证。
}
