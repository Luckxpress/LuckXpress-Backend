package com.luckxpress.service.wallet;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet balance information
 * CRITICAL: Separate Gold and Sweeps balances
 */
@Data
@Builder
public class WalletBalance {
    
    private final String userId;
    private final BigDecimal goldBalance;
    private final BigDecimal sweepsBalance;
    private final BigDecimal goldLocked;
    private final BigDecimal sweepsLocked;
    private final BigDecimal availableGold;
    private final BigDecimal availableSweeps;
    
    @Builder.Default
    private final Instant timestamp = Instant.now();
    
    /**
     * Get total value (Gold + Sweeps) for display purposes only
     * CRITICAL: This is NOT for financial calculations
     */
    public BigDecimal getTotalDisplayValue() {
        return goldBalance.add(sweepsBalance);
    }
    
    /**
     * Check if user has any locked funds
     */
    public boolean hasLockedFunds() {
        return goldLocked.compareTo(BigDecimal.ZERO) > 0 || 
               sweepsLocked.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if user can afford a Gold transaction
     */
    public boolean canAffordGold(BigDecimal amount) {
        return availableGold.compareTo(amount) >= 0;
    }
    
    /**
     * Check if user can afford a Sweeps transaction
     */
    public boolean canAffordSweeps(BigDecimal amount) {
        return availableSweeps.compareTo(amount) >= 0;
    }
}
