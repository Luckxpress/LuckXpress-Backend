package com.luckxpress.data.entity;

import com.luckxpress.common.constants.CurrencyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Account entity for user currency accounts
 * CRITICAL: Manages Gold Coins and Sweeps Coins separately per compliance
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_user", columnList = "user_id"),
    @Index(name = "idx_account_currency", columnList = "currency_type"),
    @Index(name = "idx_account_status", columnList = "status"),
    @Index(name = "idx_account_user_currency", columnList = "user_id, currency_type", unique = true)
})
@Audited
@Getter
@Setter
public class Account extends AuditableEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type", nullable = false, length = 20)
    private CurrencyType currencyType;
    
    @NotNull
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @NotNull
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance = BigDecimal.ZERO;
    
    @NotNull
    @Column(name = "pending_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingBalance = BigDecimal.ZERO;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;
    
    @Column(name = "frozen_until")
    private Instant frozenUntil;
    
    @Column(name = "last_transaction_at")
    private Instant lastTransactionAt;
    
    @Column(name = "daily_deposit_total", precision = 19, scale = 4)
    private BigDecimal dailyDepositTotal = BigDecimal.ZERO;
    
    @Column(name = "daily_withdrawal_total", precision = 19, scale = 4)
    private BigDecimal dailyWithdrawalTotal = BigDecimal.ZERO;
    
    @Column(name = "daily_reset_date")
    private Instant dailyResetDate;
    
    @Column(name = "lifetime_deposits", precision = 19, scale = 4)
    private BigDecimal lifetimeDeposits = BigDecimal.ZERO;
    
    @Column(name = "lifetime_withdrawals", precision = 19, scale = 4)
    private BigDecimal lifetimeWithdrawals = BigDecimal.ZERO;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Account status enumeration
     */
    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        FROZEN,
        CLOSED
    }
    
    /**
     * Check if account is active
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(status);
    }
    
    /**
     * Check if account is frozen
     */
    public boolean isFrozen() {
        return AccountStatus.FROZEN.equals(status) || 
               (frozenUntil != null && frozenUntil.isAfter(Instant.now()));
    }
    
    /**
     * Check if account can be debited
     */
    public boolean canDebit(BigDecimal amount) {
        return isActive() && 
               !isFrozen() && 
               availableBalance.compareTo(amount) >= 0;
    }
    
    /**
     * Check if account can be credited
     */
    public boolean canCredit() {
        return isActive() && !isFrozen();
    }
    
    /**
     * Check if withdrawals are allowed for this currency type
     * COMPLIANCE: Only Sweeps Coins are withdrawable
     */
    public boolean canWithdraw() {
        return isActive() && 
               !isFrozen() && 
               currencyType.isWithdrawable() &&
               user.canWithdraw();
    }
    
    /**
     * Get account display name
     */
    public String getDisplayName() {
        return user.getUsername() + " - " + currencyType.getDisplayName();
    }
    
    /**
     * Update daily totals (should be called daily)
     */
    public void resetDailyTotals() {
        this.dailyDepositTotal = BigDecimal.ZERO;
        this.dailyWithdrawalTotal = BigDecimal.ZERO;
        this.dailyResetDate = Instant.now();
    }
    
    /**
     * Add to daily deposit total
     */
    public void addDailyDeposit(BigDecimal amount) {
        if (this.dailyDepositTotal == null) {
            this.dailyDepositTotal = BigDecimal.ZERO;
        }
        this.dailyDepositTotal = this.dailyDepositTotal.add(amount);
        
        if (this.lifetimeDeposits == null) {
            this.lifetimeDeposits = BigDecimal.ZERO;
        }
        this.lifetimeDeposits = this.lifetimeDeposits.add(amount);
    }
    
    /**
     * Add to daily withdrawal total
     */
    public void addDailyWithdrawal(BigDecimal amount) {
        if (this.dailyWithdrawalTotal == null) {
            this.dailyWithdrawalTotal = BigDecimal.ZERO;
        }
        this.dailyWithdrawalTotal = this.dailyWithdrawalTotal.add(amount);
        
        if (this.lifetimeWithdrawals == null) {
            this.lifetimeWithdrawals = BigDecimal.ZERO;
        }
        this.lifetimeWithdrawals = this.lifetimeWithdrawals.add(amount);
    }
    
    /**
     * Freeze account until specified time
     */
    public void freezeUntil(Instant until) {
        this.status = AccountStatus.FROZEN;
        this.frozenUntil = until;
    }
    
    /**
     * Unfreeze account
     */
    public void unfreeze() {
        this.status = AccountStatus.ACTIVE;
        this.frozenUntil = null;
    }
    
    /**
     * Record transaction timestamp
     */
    public void recordTransaction() {
        this.lastTransactionAt = Instant.now();
    }
    
    @Override
    public String toString() {
        return "Account{" +
               "id='" + getId() + '\'' +
               ", userId='" + (user != null ? user.getId() : null) + '\'' +
               ", currencyType=" + currencyType +
               ", balance=" + balance +
               ", availableBalance=" + availableBalance +
               ", status=" + status +
               '}';
    }
}
