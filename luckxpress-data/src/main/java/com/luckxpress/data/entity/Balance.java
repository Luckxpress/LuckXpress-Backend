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
 * Balance snapshot entity
 * CRITICAL: Maintains balance history for audit and reconciliation
 */
@Entity
@Table(name = "balances", indexes = {
    @Index(name = "idx_balance_account", columnList = "account_id"),
    @Index(name = "idx_balance_transaction", columnList = "transaction_id"),
    @Index(name = "idx_balance_snapshot", columnList = "snapshot_at"),
    @Index(name = "idx_balance_account_snapshot", columnList = "account_id, snapshot_at")
})
@Audited
@Getter
@Setter
public class Balance extends BaseEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type", nullable = false, length = 20)
    private CurrencyType currencyType;
    
    @NotNull
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;
    
    @NotNull
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;
    
    @NotNull
    @Column(name = "available_balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalanceBefore;
    
    @NotNull
    @Column(name = "available_balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalanceAfter;
    
    @NotNull
    @Column(name = "pending_balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingBalanceBefore;
    
    @NotNull
    @Column(name = "pending_balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingBalanceAfter;
    
    @NotNull
    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 30)
    private SnapshotType snapshotType;
    
    @Column(name = "change_amount", precision = 19, scale = 4)
    private BigDecimal changeAmount;
    
    @Column(name = "change_reason", length = 500)
    private String changeReason;
    
    @Column(name = "reconciliation_id", length = 26)
    private String reconciliationId;
    
    /**
     * Balance snapshot type enumeration
     */
    public enum SnapshotType {
        TRANSACTION,        // Balance change due to transaction
        DAILY_SNAPSHOT,     // Daily balance snapshot
        RECONCILIATION,     // Balance reconciliation
        ADJUSTMENT,         // Manual balance adjustment
        SYSTEM_CORRECTION,  // System-initiated correction
        AUDIT_SNAPSHOT      // Audit-triggered snapshot
    }
    
    /**
     * Calculate balance change
     */
    public BigDecimal getBalanceChange() {
        return balanceAfter.subtract(balanceBefore);
    }
    
    /**
     * Calculate available balance change
     */
    public BigDecimal getAvailableBalanceChange() {
        return availableBalanceAfter.subtract(availableBalanceBefore);
    }
    
    /**
     * Calculate pending balance change
     */
    public BigDecimal getPendingBalanceChange() {
        return pendingBalanceAfter.subtract(pendingBalanceBefore);
    }
    
    /**
     * Check if this is a transaction-related snapshot
     */
    public boolean isTransactionSnapshot() {
        return SnapshotType.TRANSACTION.equals(snapshotType) && transaction != null;
    }
    
    /**
     * Check if this is a daily snapshot
     */
    public boolean isDailySnapshot() {
        return SnapshotType.DAILY_SNAPSHOT.equals(snapshotType);
    }
    
    /**
     * Check if this is a reconciliation snapshot
     */
    public boolean isReconciliationSnapshot() {
        return SnapshotType.RECONCILIATION.equals(snapshotType);
    }
    
    /**
     * Check if balance increased
     */
    public boolean isBalanceIncrease() {
        return getBalanceChange().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if balance decreased
     */
    public boolean isBalanceDecrease() {
        return getBalanceChange().compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Get user ID from account
     */
    public String getUserId() {
        return account != null && account.getUser() != null ? 
               account.getUser().getId() : null;
    }
    
    /**
     * Get transaction ID if available
     */
    public String getTransactionId() {
        return transaction != null ? transaction.getId() : null;
    }
    
    /**
     * Create balance snapshot from account state
     */
    public static Balance createSnapshot(Account account, 
                                       Transaction transaction, 
                                       SnapshotType type,
                                       BigDecimal balanceBefore,
                                       BigDecimal availableBalanceBefore,
                                       BigDecimal pendingBalanceBefore) {
        Balance balance = new Balance();
        balance.setAccount(account);
        balance.setTransaction(transaction);
        balance.setCurrencyType(account.getCurrencyType());
        balance.setSnapshotType(type);
        balance.setSnapshotAt(Instant.now());
        
        // Before balances
        balance.setBalanceBefore(balanceBefore);
        balance.setAvailableBalanceBefore(availableBalanceBefore);
        balance.setPendingBalanceBefore(pendingBalanceBefore);
        
        // After balances (current account state)
        balance.setBalanceAfter(account.getBalance());
        balance.setAvailableBalanceAfter(account.getAvailableBalance());
        balance.setPendingBalanceAfter(account.getPendingBalance());
        
        // Calculate change amount
        balance.setChangeAmount(balance.getBalanceChange());
        
        if (transaction != null) {
            balance.setChangeReason("Transaction: " + transaction.getTransactionType().getDescription());
        }
        
        return balance;
    }
    
    /**
     * Create daily snapshot
     */
    public static Balance createDailySnapshot(Account account) {
        Balance balance = new Balance();
        balance.setAccount(account);
        balance.setCurrencyType(account.getCurrencyType());
        balance.setSnapshotType(SnapshotType.DAILY_SNAPSHOT);
        balance.setSnapshotAt(Instant.now());
        
        // For daily snapshots, before and after are the same (current state)
        balance.setBalanceBefore(account.getBalance());
        balance.setBalanceAfter(account.getBalance());
        balance.setAvailableBalanceBefore(account.getAvailableBalance());
        balance.setAvailableBalanceAfter(account.getAvailableBalance());
        balance.setPendingBalanceBefore(account.getPendingBalance());
        balance.setPendingBalanceAfter(account.getPendingBalance());
        
        balance.setChangeAmount(BigDecimal.ZERO);
        balance.setChangeReason("Daily balance snapshot");
        
        return balance;
    }
    
    @Override
    public String toString() {
        return "Balance{" +
               "id='" + getId() + '\'' +
               ", accountId='" + (account != null ? account.getId() : null) + '\'' +
               ", currencyType=" + currencyType +
               ", balanceBefore=" + balanceBefore +
               ", balanceAfter=" + balanceAfter +
               ", snapshotType=" + snapshotType +
               ", snapshotAt=" + snapshotAt +
               '}';
    }
}
