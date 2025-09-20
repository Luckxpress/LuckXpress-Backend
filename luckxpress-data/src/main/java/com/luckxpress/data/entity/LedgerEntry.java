package com.luckxpress.data.entity;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.common.validation.ValidMoney;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ledger Entry entity
 * CRITICAL: Immutable financial ledger - NEVER UPDATE, only INSERT
 */
@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_user", columnList = "user_id"),
    @Index(name = "idx_ledger_account", columnList = "account_id"),
    @Index(name = "idx_ledger_transaction", columnList = "transaction_id"),
    @Index(name = "idx_ledger_type", columnList = "entry_type"),
    @Index(name = "idx_ledger_currency", columnList = "currency_type"),
    @Index(name = "idx_ledger_posted", columnList = "posted_at"),
    @Index(name = "idx_ledger_reference", columnList = "reference_number", unique = true),
    @Index(name = "idx_ledger_reconciliation", columnList = "reconciliation_batch")
})
@Audited
@Getter
@Setter
public class LedgerEntry extends BaseEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    private String referenceNumber;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type", nullable = false, length = 20)
    private CurrencyType currencyType;
    
    @NotNull
    @ValidMoney
    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal debitAmount = BigDecimal.ZERO;
    
    @NotNull
    @ValidMoney
    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditAmount = BigDecimal.ZERO;
    
    @NotNull
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;
    
    @NotNull
    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;
    
    @NotNull
    @Size(max = 500)
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    
    @Size(max = 100)
    @Column(name = "external_reference", length = 100)
    private String externalReference;
    
    @Size(max = 50)
    @Column(name = "reconciliation_batch", length = 50)
    private String reconciliationBatch;
    
    @Column(name = "reconciled_at")
    private Instant reconciledAt;
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    @Size(max = 26)
    @Column(name = "posted_by", length = 26)
    private String postedBy;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Size(max = 26)
    @Column(name = "reversal_entry_id", length = 26)
    private String reversalEntryId;
    
    @Column(name = "is_reversal", nullable = false)
    private Boolean isReversal = false;
    
    /**
     * Ledger entry type enumeration
     */
    public enum EntryType {
        DEBIT,   // Decreases balance
        CREDIT   // Increases balance
    }
    
    /**
     * Check if entry is debit
     */
    public boolean isDebit() {
        return EntryType.DEBIT.equals(entryType);
    }
    
    /**
     * Check if entry is credit
     */
    public boolean isCredit() {
        return EntryType.CREDIT.equals(entryType);
    }
    
    /**
     * Get effective amount (positive for credits, negative for debits)
     */
    public BigDecimal getEffectiveAmount() {
        if (isCredit()) {
            return creditAmount;
        } else {
            return debitAmount.negate();
        }
    }
    
    /**
     * Get the non-zero amount
     */
    public BigDecimal getAmount() {
        return isCredit() ? creditAmount : debitAmount;
    }
    
    /**
     * Check if entry is reconciled
     */
    public boolean isReconciled() {
        return reconciledAt != null && reconciliationBatch != null;
    }
    
    /**
     * Check if entry is a reversal
     */
    public boolean isReversalEntry() {
        return Boolean.TRUE.equals(isReversal);
    }
    
    /**
     * Mark entry as reconciled
     */
    public void markReconciled(String batchId) {
        this.reconciliationBatch = batchId;
        this.reconciledAt = Instant.now();
    }
    
    /**
     * Create debit ledger entry
     */
    public static LedgerEntry createDebit(User user, 
                                        Account account, 
                                        Transaction transaction,
                                        BigDecimal amount, 
                                        BigDecimal balanceAfter,
                                        String description) {
        LedgerEntry entry = new LedgerEntry();
        entry.setUser(user);
        entry.setAccount(account);
        entry.setTransaction(transaction);
        entry.setEntryType(EntryType.DEBIT);
        entry.setTransactionType(transaction.getTransactionType());
        entry.setCurrencyType(account.getCurrencyType());
        entry.setDebitAmount(amount);
        entry.setCreditAmount(BigDecimal.ZERO);
        entry.setBalanceAfter(balanceAfter);
        entry.setDescription(description);
        entry.setPostedAt(Instant.now());
        entry.setReferenceNumber(generateReferenceNumber());
        
        return entry;
    }
    
    /**
     * Create credit ledger entry
     */
    public static LedgerEntry createCredit(User user, 
                                         Account account, 
                                         Transaction transaction,
                                         BigDecimal amount, 
                                         BigDecimal balanceAfter,
                                         String description) {
        LedgerEntry entry = new LedgerEntry();
        entry.setUser(user);
        entry.setAccount(account);
        entry.setTransaction(transaction);
        entry.setEntryType(EntryType.CREDIT);
        entry.setTransactionType(transaction.getTransactionType());
        entry.setCurrencyType(account.getCurrencyType());
        entry.setDebitAmount(BigDecimal.ZERO);
        entry.setCreditAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setDescription(description);
        entry.setPostedAt(Instant.now());
        entry.setReferenceNumber(generateReferenceNumber());
        
        return entry;
    }
    
    /**
     * Create reversal entry
     */
    public LedgerEntry createReversal(BigDecimal newBalanceAfter, String reversalReason) {
        LedgerEntry reversal = new LedgerEntry();
        reversal.setUser(this.user);
        reversal.setAccount(this.account);
        reversal.setTransaction(this.transaction);
        reversal.setCurrencyType(this.currencyType);
        reversal.setTransactionType(this.transactionType);
        reversal.setBalanceAfter(newBalanceAfter);
        reversal.setPostedAt(Instant.now());
        reversal.setReferenceNumber(generateReferenceNumber());
        reversal.setIsReversal(true);
        reversal.setReversalEntryId(this.getId());
        
        // Reverse the amounts
        if (this.isDebit()) {
            reversal.setEntryType(EntryType.CREDIT);
            reversal.setCreditAmount(this.debitAmount);
            reversal.setDebitAmount(BigDecimal.ZERO);
        } else {
            reversal.setEntryType(EntryType.DEBIT);
            reversal.setDebitAmount(this.creditAmount);
            reversal.setCreditAmount(BigDecimal.ZERO);
        }
        
        reversal.setDescription("REVERSAL: " + reversalReason + " (Original: " + this.description + ")");
        
        return reversal;
    }
    
    /**
     * Generate unique reference number
     */
    private static String generateReferenceNumber() {
        return "LDG-" + System.currentTimeMillis() + "-" + 
               String.format("%04d", (int)(Math.random() * 10000));
    }
    
    @Override
    public String toString() {
        return "LedgerEntry{" +
               "id='" + getId() + '\'' +
               ", referenceNumber='" + referenceNumber + '\'' +
               ", entryType=" + entryType +
               ", transactionType=" + transactionType +
               ", currencyType=" + currencyType +
               ", debitAmount=" + debitAmount +
               ", creditAmount=" + creditAmount +
               ", balanceAfter=" + balanceAfter +
               ", postedAt=" + postedAt +
               '}';
    }
}
