package com.aipick.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * 静态资源：默认封面图等，从 classpath 提供
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/static")
public class StaticController {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_.-]+\\.(png|jpg|jpeg|gif|webp)$");

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
