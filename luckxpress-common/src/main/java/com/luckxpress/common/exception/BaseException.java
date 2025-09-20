package com.luckxpress.common.exception;

import lombok.Getter;
import java.time.Instant;

/**
 * Base exception for all custom exceptions
 */
@Getter
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final Instant timestamp;
    private final String traceId;
    
    protected BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.traceId = generateTraceId();
    }
    
    protected BaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.traceId = generateTraceId();
    }
    
    private String generateTraceId() {
        return "TRC-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
}
