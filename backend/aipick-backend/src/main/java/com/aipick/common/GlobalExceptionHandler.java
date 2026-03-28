package com.aipick.common;

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
        return Result.error(e.getCode(), e.getMessage());
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
