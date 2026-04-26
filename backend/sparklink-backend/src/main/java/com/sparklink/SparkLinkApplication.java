package com.sparklink;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spark Link 后端应用启动类
 */
@SpringBootApplication
@MapperScan("com.sparklink.mapper")
public class SparkLinkApplication {

    public static void main(String[] args) {
        loadEnvFromDotFile();
        SpringApplication.run(SparkLinkApplication.class, args);
        System.out.println("✅ Spark Link 后端服务启动成功！");
    }

    /**
     * 在 Spring 环境装配前从 .env 读入键值，写入系统属性，供 application.yml 中 ${WECHAT_SECRET:} 等使用。
     * 已存在于操作系统环境变量中的键不会覆盖。优先在仓库子目录、其次当前工作目录查找 .env。
     */
    private static void loadEnvFromDotFile() {
        Path[] candidates = new Path[] {
            Paths.get("backend", "sparklink-backend", ".env"),
            Paths.get(".env")
        };
        for (Path envFile : candidates) {
            if (!Files.isRegularFile(envFile)) {
                continue;
            }
            String dir = envFile.getParent() != null
                ? envFile.getParent().toAbsolutePath().toString()
                : Paths.get("").toAbsolutePath().toString();
            Dotenv dotenv = Dotenv.configure()
                .directory(dir)
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                if (key == null || key.isEmpty()) {
                    return;
                }
                if (System.getenv(key) != null) {
                    return;
                }
                if (System.getProperty(key) != null) {
                    return;
                }
                String val = entry.getValue();
                if (val != null) {
                    System.setProperty(key, val);
                }
            });
            return;
        }
    }
}