package com.luckxpress.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Wallet entity with dual currency
 * CRITICAL: Gold and Sweeps MUST be separate, Gold is NEVER withdrawable
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallets_user_id", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {
    
    @Column(name = "user_id", nullable = false, unique = true, length = 32)
    private String userId;
    
    @Column(name = "gold_balance", nullable = false, precision = 15, scale = 4)
    private BigDecimal goldBalance = BigDecimal.ZERO;
    
    @Column(name = "sweeps_balance", nullable = false, precision = 15, scale = 4)
    private BigDecimal sweepsBalance = BigDecimal.ZERO;
    
    @Column(name = "gold_locked", nullable = false, precision = 15, scale = 4)
    private BigDecimal goldLocked = BigDecimal.ZERO;
    
    @Column(name = "sweeps_locked", nullable = false, precision = 15, scale = 4)
    private BigDecimal sweepsLocked = BigDecimal.ZERO;
    
    @Column(name = "total_gold_wagered", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalGoldWagered = BigDecimal.ZERO;
    
    @Column(name = "total_sweeps_wagered", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalSweepsWagered = BigDecimal.ZERO;
    
    @Column(name = "total_gold_won", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalGoldWon = BigDecimal.ZERO;
    
    @Column(name = "total_sweeps_won", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalSweepsWon = BigDecimal.ZERO;
    
    @Override
    protected String getEntityPrefix() {
        return "WLT";
    }
    
    /**
     * Get available gold balance (excluding locked)
     */
    public BigDecimal getAvailableGoldBalance() {
        return goldBalance.subtract(goldLocked);
    }
    
    /**
     * Get available sweeps balance (excluding locked)
     */
    public BigDecimal getAvailableSweepsBalance() {
        return sweepsBalance.subtract(sweepsLocked);
    }
    
    /**
     * Validate sufficient balance
     */
    public boolean hasSufficientGoldBalance(BigDecimal amount) {
        return getAvailableGoldBalance().compareTo(amount) >= 0;
    }
    
    public boolean hasSufficientSweepsBalance(BigDecimal amount) {
        return getAvailableSweepsBalance().compareTo(amount) >= 0;
    }
}
