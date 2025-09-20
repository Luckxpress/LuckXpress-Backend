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
 * Transaction entity
 * CRITICAL: Records all financial transactions with full audit trail
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_user", columnList = "user_id"),
    @Index(name = "idx_transaction_account", columnList = "account_id"),
    @Index(name = "idx_transaction_type", columnList = "transaction_type"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_idempotency", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_transaction_reference", columnList = "external_reference"),
    @Index(name = "idx_transaction_created", columnList = "created_at"),
    @Index(name = "idx_transaction_processed", columnList = "processed_at")
})
@Audited
@Getter
@Setter
public class Transaction extends AuditableEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
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
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    
    @Size(max = 100)
    @Column(name = "external_reference", length = 100)
    private String externalReference;
    
    @Size(max = 100)
    @Column(name = "payment_method", length = 100)
    private String paymentMethod;
    
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Column(name = "failed_at")
    private Instant failedAt;
    
    @Size(max = 1000)
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    
    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;
    
    @Column(name = "approved_at")
    private Instant approvedAt;
    
    @Size(max = 26)
    @Column(name = "approved_by", length = 26)
    private String approvedBy;
    
    @Size(max = 1000)
    @Column(name = "approval_notes", length = 1000)
    private String approvalNotes;
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Size(max = 26)
    @Column(name = "related_transaction_id", length = 26)
    private String relatedTransactionId;
    
    /**
     * Transaction status enumeration
     */
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        REVERSED
    }
    
    /**
     * Check if transaction is completed
     */
    public boolean isCompleted() {
        return TransactionStatus.COMPLETED.equals(status);
    }
    
    /**
     * Check if transaction is pending
     */
    public boolean isPending() {
        return TransactionStatus.PENDING.equals(status) || 
               TransactionStatus.PROCESSING.equals(status);
    }
    
    /**
     * Check if transaction failed
     */
    public boolean isFailed() {
        return TransactionStatus.FAILED.equals(status) || 
               TransactionStatus.CANCELLED.equals(status) || 
               TransactionStatus.REJECTED.equals(status);
    }
    
    /**
     * Check if transaction is credit (increases balance)
     */
    public boolean isCredit() {
        return transactionType != null && transactionType.isCredit();
    }
    
    /**
     * Check if transaction is debit (decreases balance)
     */
    public boolean isDebit() {
        return transactionType != null && !transactionType.isCredit();
    }
    
    /**
     * Check if transaction requires approval
     */
    public boolean needsApproval() {
        return Boolean.TRUE.equals(requiresApproval) && 
               !TransactionStatus.APPROVED.equals(status) &&
               !TransactionStatus.REJECTED.equals(status);
    }
    
    /**
     * Check if transaction is approved
     */
    public boolean isApproved() {
        return TransactionStatus.APPROVED.equals(status) || approvedAt != null;
    }
    
    /**
     * Mark transaction as completed
     */
    public void markCompleted(BigDecimal balanceBefore, BigDecimal balanceAfter) {
        this.status = TransactionStatus.COMPLETED;
        this.processedAt = Instant.now();
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
    }
    
    /**
     * Mark transaction as failed
     */
    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failedAt = Instant.now();
        this.failureReason = reason;
    }
    
    /**
     * Mark transaction as requiring approval
     */
    public void markRequiresApproval() {
        this.status = TransactionStatus.PENDING_APPROVAL;
        this.requiresApproval = true;
    }
    
    /**
     * Approve transaction
     */
    public void approve(String approvedByUserId, String notes) {
        this.status = TransactionStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.approvedBy = approvedByUserId;
        this.approvalNotes = notes;
    }
    
    /**
     * Reject transaction
     */
    public void reject(String rejectedByUserId, String reason) {
        this.status = TransactionStatus.REJECTED;
        this.approvedBy = rejectedByUserId;
        this.approvalNotes = reason;
        this.failedAt = Instant.now();
        this.failureReason = "Rejected: " + reason;
    }
    
    /**
     * Get effective amount (positive for credits, negative for debits)
     */
    public BigDecimal getEffectiveAmount() {
        return isCredit() ? amount : amount.negate();
    }
    
    /**
     * Get transaction display description
     */
    public String getDisplayDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        return transactionType.getDescription() + " - " + 
               currencyType.getDisplayName() + " " + amount;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
               "id='" + getId() + '\'' +
               ", userId='" + (user != null ? user.getId() : null) + '\'' +
               ", transactionType=" + transactionType +
               ", currencyType=" + currencyType +
               ", amount=" + amount +
               ", status=" + status +
               ", idempotencyKey='" + idempotencyKey + '\'' +
               '}';
    }
}
