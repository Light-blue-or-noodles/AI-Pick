package com.sparklink.common;

import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import org.springframework.dao.DataAccessException;

import java.io.IOException;

/**
 * 全局异常处理器
 *
 * @author AI-Pick
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验失败：{}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.warn("参数绑定失败：{}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理缺少请求头异常（如缺少 X-User-Id 时提示请先登录）
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        if ("X-User-Id".equals(e.getHeaderName())) {
            log.warn("缺少登录信息：未提供 X-User-Id 请求头");
            return Result.unauthorized("请先登录");
        }
        log.warn("缺少请求头：{}", e.getHeaderName());
        return Result.badRequest("缺少必要请求头：" + e.getHeaderName());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{} (code={})", e.getMessage(), e.getCode());
        // 与 HTTP 400 一致：旧代码用 error(getCode) 时默认 code=500 会导致 JSON 的 code 与状态码矛盾
        return Result.badRequest(e.getMessage());
    }

    /**
     * 单文件超 spring.servlet.multipart.max-file-size 时，先于 Controller 抛出（勿与「未选文件」混为一谈）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        long max = e.getMaxUploadSize() > 0 ? e.getMaxUploadSize() : 0L;
        log.warn("上传超过大小限制: maxBytes={}", max);
        return Result.badRequest("单张图片超过大小限制（最大约 5MB），请换一张较小的图片或先压缩后再上传。");
    }

    /**
     * 未带 multipart 文件、或 Content-Type/边界不合法时，Spring 抛 MultipartException
     * （注意：包体过大在多数情况下会走 MaxUploadSizeExceededException，少数边界情况仍进此处）
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMultipartException(MultipartException e) {
        log.warn("Multipart 解析/上传失败: {} ({})", e.getMessage(), e.getClass().getName());
        Throwable cause = e.getCause();
        if (cause != null) {
            String c = (cause.getClass().getName() + (cause.getMessage() != null ? cause.getMessage() : "")).toLowerCase();
            if (c.contains("sizelimitexceeded") || c.contains("exceeds") || c.contains("size limit")) {
                return Result.badRequest("文件超过大小限制，请使用 5MB 以内的图片。");
            }
        }
        return Result.badRequest(
                "请使用 multipart/form-data 上传，字段名 file 或 image。"
                        + " 示例: curl -F \"file=@/绝对路径/photo.jpg\" -H \"Authorization: Bearer <token>\" -H \"X-User-Id: 23\" http://<host>:8080/api/user/avatar");
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据库异常", e);
        String cause = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        return Result.error(500, "数据保存失败，请检查数据库与日志：" + (cause != null ? cause : "unknown"));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());
        return Result.badRequest(e.getMessage());
    }

    /**
     * 请求体不是合法 JSON、或无法绑定到方法参数时（小程序端 Content-Type/body 不一致时常见）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String detail = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        log.warn("请求体解析失败：{}", detail);
        return Result.badRequest("请求体格式错误，请使用 application/json 发送合法 JSON（例如 {\"peerUserId\":10}）");
    }

    /**
     * X-User-Id 等请求头无法转为 Long 时（异常保存在 Servlet 包装异常之前先在此处理）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String param = e.getName() != null ? e.getName() : "";
        log.warn("参数类型不匹配 {} : {}", param, e.getMessage());
        if ("viewerId".equals(param) || "userId".equalsIgnoreCase(param)) {
            return Result.badRequest("当前用户 ID（X-User-Id）格式无效，请重新登录");
        }
        return Result.badRequest("参数无效：" + param);
    }

    /**
     * Servlet 容器或 Spring MVC 在包装根因时抛出的受检异常，避免仅返回「未知异常」
     */
    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleServletException(ServletException e) {
        log.error("Servlet 异常: {}", e.getMessage(), e);
        Throwable c = e.getCause();
        if (c instanceof RuntimeException && c.getMessage() != null && !c.getMessage().isBlank()) {
            return Result.error(500, "请求处理失败: " + c.getMessage());
        }
        if (c != null) {
            log.error("Servlet 异常 cause: {} ({})", c.getMessage(), c.getClass().getName());
        }
        return Result.error("未知异常，请联系管理员");
    }

    /**
     * 客户端中断连接、或写出响应时的 IO 问题
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleIOException(IOException e) {
        log.error("IO 异常", e);
        return Result.error("网络传输异常，请重试");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请稍后重试");
    }

    /**
     * 处理所有异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return Result.error("未知异常，请联系管理员");
    }
}
