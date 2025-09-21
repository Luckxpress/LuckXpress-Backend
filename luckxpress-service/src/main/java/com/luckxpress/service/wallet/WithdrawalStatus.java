package com.luckxpress.service.wallet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Withdrawal processing status
 * CRITICAL: Track withdrawal lifecycle for compliance
 */
@Getter
@RequiredArgsConstructor
public enum WithdrawalStatus {
    
    PENDING("pending", "Withdrawal request submitted"),
    PROCESSING("processing", "Payment processing in progress"),
    COMPLETED("completed", "Withdrawal completed successfully"),
    FAILED("failed", "Withdrawal failed"),
    CANCELLED("cancelled", "Withdrawal cancelled by user"),
    REJECTED("rejected", "Withdrawal rejected by compliance");
    
    private final String code;
    private final String description;
    
    public static WithdrawalStatus fromCode(String code) {
        for (WithdrawalStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid withdrawal status: " + code);
    }
}
