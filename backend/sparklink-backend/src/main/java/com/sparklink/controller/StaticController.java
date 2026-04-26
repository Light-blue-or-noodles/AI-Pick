package com.sparklink.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 静态资源：默认封面图等，从 classpath 提供
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/static")
public class StaticController {

    private static final Logger log = LoggerFactory.getLogger(StaticController.class);

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_.-]+\\.(png|jpg|jpeg|gif|webp)$");

    @Value("${storage.local.root-dir:${upload.dir:uploads}}")
    private String uploadRootDir;

    @Value("${upload.avatar-subdir:avatars}")
    private String avatarSubdir;

    /**
     * 用户上传的头像：GET /api/static/avatars/{filename}
     * 显式从磁盘流式返回，与 LocalImageStorageService 写入路径一致，避免仅依赖 file: Resource 映射时偶发 500。
     */
    @GetMapping("/avatars/{filename}")
    public void getAvatar(@PathVariable String filename, HttpServletResponse response) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Path base = Paths.get(uploadRootDir).toAbsolutePath().normalize().resolve(avatarSubdir);
        try {
            Path file = base.resolve(filename).normalize();
            if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String contentType = filename.toLowerCase().endsWith(".png") ? "image/png"
                    : filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")
                    ? "image/jpeg" : filename.toLowerCase().endsWith(".gif") ? "image/gif"
                    : filename.toLowerCase().endsWith(".webp") ? "image/webp" : "application/octet-stream";
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=86400");
            try (InputStream in = Files.newInputStream(file)) {
                in.transferTo(response.getOutputStream());
            }
            response.flushBuffer();
        } catch (Exception e) {
            log.error("读取头像文件失败: {}", filePathSafe(base, filename), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to read avatar");
            } catch (Exception ignored) {
            }
        }
    }

    private static String filePathSafe(Path base, String filename) {
        try {
            return base.resolve(filename).normalize().toString();
        } catch (Exception e) {
            return filename;
        }
    }

    /**
     * 获取默认封面图：GET /api/static/covers/{filename}
     */
    @GetMapping("/covers/{filename}")
    public void getCover(@PathVariable String filename, HttpServletResponse response) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String path = "static/covers/" + filename;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String contentType = filename.toLowerCase().endsWith(".png") ? "image/png"
                    : filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg") ? "image/jpeg"
                    : filename.toLowerCase().endsWith(".gif") ? "image/gif"
                    : filename.toLowerCase().endsWith(".webp") ? "image/webp"
                    : "application/octet-stream";
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=86400");
            in.transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to read resource");
            } catch (Exception ignored) {
            }
        }
    }
}
