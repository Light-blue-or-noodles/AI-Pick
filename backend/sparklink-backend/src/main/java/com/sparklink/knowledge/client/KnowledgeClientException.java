package com.sparklink.knowledge.client;

/**
 * 知识客户端统一异常。
 */
public class KnowledgeClientException extends RuntimeException {

    private final ErrorType errorType;

    public KnowledgeClientException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public enum ErrorType {
        HTTP_ERROR,
        PARSE_ERROR,
        SERIALIZE_ERROR
    }
}
