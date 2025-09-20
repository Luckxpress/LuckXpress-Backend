package com.luckxpress.web.dto;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.data.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction Response DTO
 * CRITICAL: Transaction data for API responses
 */
@Data
public class TransactionResponseDto {
    
    private String id;
    private String userId;
    private String accountId;
    private TransactionType transactionType;
    private CurrencyType currencyType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Transaction.TransactionStatus status;
    private String idempotencyKey;
    private String externalReference;
    private String paymentMethod;
    private String description;
    private Instant createdAt;
    private Instant processedAt;
    private Instant failedAt;
    private String failureReason;
    private Boolean requiresApproval;
    private Instant approvedAt;
    private String approvedBy;
    private String approvalNotes;
    private String relatedTransactionId;
    
    public static TransactionResponseDto fromTransaction(Transaction transaction) {
        TransactionResponseDto dto = new TransactionResponseDto();
        dto.setId(transaction.getId());
        dto.setUserId(transaction.getUser().getId());
        dto.setAccountId(transaction.getAccount().getId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setCurrencyType(transaction.getCurrencyType());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceBefore(transaction.getBalanceBefore());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setStatus(transaction.getStatus());
        dto.setIdempotencyKey(transaction.getIdempotencyKey());
        dto.setExternalReference(transaction.getExternalReference());
        dto.setPaymentMethod(transaction.getPaymentMethod());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setProcessedAt(transaction.getProcessedAt());
        dto.setFailedAt(transaction.getFailedAt());
        dto.setFailureReason(transaction.getFailureReason());
        dto.setRequiresApproval(transaction.getRequiresApproval());
        dto.setApprovedAt(transaction.getApprovedAt());
        dto.setApprovedBy(transaction.getApprovedBy());
        dto.setApprovalNotes(transaction.getApprovalNotes());
        dto.setRelatedTransactionId(transaction.getRelatedTransactionId());
        return dto;
    }
}
