package com.luckxpress.data.entity;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.common.util.IdGenerator;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Immutable ledger entry for transaction history
 * CRITICAL: NEVER allow updates to ledger entries
 */
@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_user_currency", columnList = "user_id, currency"),
    @Index(name = "idx_ledger_created_at", columnList = "created_at"),
    @Index(name = "idx_ledger_reference", columnList = "reference_id"),
    @Index(name = "idx_ledger_type", columnList = "transaction_type")
})
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {
    
    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;
    
    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private CurrencyType currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "balance_before", nullable = false, precision = 15, scale = 4)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 4)
    private BigDecimal balanceAfter;
    
    @Column(name = "reference_id", length = 100)
    private String referenceId;
    
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "actor", length = 100)
    private String actor;
    
    @Column(name = "actor_type", length = 20)
    private String actorType;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "correlation_id", length = 50)
    private String correlationId;
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = IdGenerator.generateId("LED");
        }
        this.createdAt = Instant.now();
    }
    
    /**
     * Validate ledger entry integrity
     */
    public boolean validateIntegrity() {
        BigDecimal expectedBalance;
        if (transactionType.isCredit()) {
            expectedBalance = balanceBefore.add(amount);
        } else {
            expectedBalance = balanceBefore.subtract(amount);
        }
        return expectedBalance.compareTo(balanceAfter) == 0;
    }
}
