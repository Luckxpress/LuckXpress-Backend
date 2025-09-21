package com.luckxpress.service.wallet;

import com.luckxpress.common.constants.CurrencyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet transaction result
 * CRITICAL: Immutable transaction record
 */
@Data
@Builder
public class WalletTransaction {
    
    private final String transactionId;
    private final String userId;
    private final CurrencyType currency;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String reference;
    
    @Builder.Default
    private final Instant timestamp = Instant.now();
    
    /**
     * Check if this is a credit transaction
     */
    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if this is a debit transaction
     */
    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Get absolute amount (always positive)
     */
    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }
}
