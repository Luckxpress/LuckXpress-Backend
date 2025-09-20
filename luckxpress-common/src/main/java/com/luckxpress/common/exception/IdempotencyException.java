package com.luckxpress.common.exception;

import lombok.Getter;

/**
 * Exception for duplicate idempotent requests
 */
@Getter
public class IdempotencyException extends BaseException {
    
    private final String idempotencyKey;
    private final String originalTransactionId;
    
    public IdempotencyException(String idempotencyKey, String originalTransactionId) {
        super(
            "DUPLICATE_REQUEST",
            String.format("Duplicate request with idempotency key: %s", idempotencyKey)
        );
        this.idempotencyKey = idempotencyKey;
        this.originalTransactionId = originalTransactionId;
    }
}
