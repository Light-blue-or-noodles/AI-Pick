package com.aipick;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI-Pick 后端应用启动类
 */
@SpringBootApplication
@MapperScan("com.aipick.mapper")
public class AIPickApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIPickApplication.class, args);
        System.out.println("✅ AI-Pick 后端服务启动成功！");
    }
}