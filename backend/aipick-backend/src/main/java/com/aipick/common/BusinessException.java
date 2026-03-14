package com.aipick.common;

/**
 * 业务异常
 *
 * @author AI-Pick
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public Integer getCode() {
        return code;
    }
}