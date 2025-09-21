package com.luckxpress.service.wallet;

import com.luckxpress.common.constants.CurrencyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Withdrawal processing result
 * CRITICAL: Immutable withdrawal record
 */
@Data
@Builder
public class WithdrawalResult {
    
    private final String withdrawalId;
    private final String transactionId;
    private final String userId;
    private final CurrencyType currency;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String paymentMethod;
    private final WithdrawalStatus status;
    private final Instant requestedAt;
    
    /**
     * Check if withdrawal is pending
     */
    public boolean isPending() {
        return status == WithdrawalStatus.PENDING;
    }
    
    /**
     * Check if withdrawal is completed
     */
    public boolean isCompleted() {
        return status == WithdrawalStatus.COMPLETED;
    }
    
    /**
     * Check if withdrawal failed
     */
    public boolean isFailed() {
        return status == WithdrawalStatus.FAILED;
    }
}
