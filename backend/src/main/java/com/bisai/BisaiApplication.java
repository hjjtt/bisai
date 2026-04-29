package com.bisai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bisai.mapper")
public class BisaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BisaiApplication.class, args);
    }
}
