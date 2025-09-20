package com.luckxpress.data.entity;

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
 * Approval Workflow entity
 * CRITICAL: Manages dual/triple approval workflows for high-value transactions
 */
@Entity
@Table(name = "approval_workflows", indexes = {
    @Index(name = "idx_approval_transaction", columnList = "transaction_id"),
    @Index(name = "idx_approval_initiator", columnList = "initiated_by"),
    @Index(name = "idx_approval_status", columnList = "status"),
    @Index(name = "idx_approval_type", columnList = "approval_type"),
    @Index(name = "idx_approval_priority", columnList = "priority"),
    @Index(name = "idx_approval_created", columnList = "created_at"),
    @Index(name = "idx_approval_expires", columnList = "expires_at")
})
@Audited
@Getter
@Setter
public class ApprovalWorkflow extends AuditableEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 30)
    private ApprovalType approvalType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.PENDING;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority = Priority.NORMAL;
    
    @NotNull
    @Size(max = 26)
    @Column(name = "initiated_by", nullable = false, length = 26)
    private String initiatedBy;
    
    @NotNull
    @ValidMoney
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @NotBlank
    @Size(max = 1000)
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;
    
    @Column(name = "business_justification", columnDefinition = "TEXT")
    private String businessJustification;
    
    @NotNull
    @Column(name = "required_approvals", nullable = false)
    private Integer requiredApprovals;
    
    @NotNull
    @Column(name = "received_approvals", nullable = false)
    private Integer receivedApprovals = 0;
    
    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Size(max = 26)
    @Column(name = "completed_by", length = 26)
    private String completedBy;
    
    @Column(name = "completion_notes", length = 1000)
    private String completionNotes;
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    /**
     * Approval type enumeration
     */
    public enum ApprovalType {
        DUAL_APPROVAL,          // Requires 2 approvals
        TRIPLE_APPROVAL,        // Requires 3 approvals
        COMPLIANCE_REVIEW,      // Compliance officer review
        FINANCE_APPROVAL,       // Finance manager approval
        EXECUTIVE_APPROVAL,     // Executive approval
        MANUAL_TRANSACTION,     // Manual transaction approval
        LARGE_WITHDRAWAL,       // Large withdrawal approval
        SUSPICIOUS_ACTIVITY,    // Suspicious activity review
        KYC_OVERRIDE,          // KYC requirement override
        STATE_OVERRIDE         // State restriction override
    }
    
    /**
     * Workflow status enumeration
     */
    public enum WorkflowStatus {
        PENDING,        // Waiting for approvals
        IN_PROGRESS,    // Being reviewed
        APPROVED,       // All approvals received
        REJECTED,       // Rejected by approver
        EXPIRED,        // Approval window expired
        CANCELLED,      // Cancelled by initiator
        ESCALATED       // Escalated to higher authority
    }
    
    /**
     * Priority enumeration
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT,
        CRITICAL
    }
    
    /**
     * Check if workflow is pending
     */
    public boolean isPending() {
        return WorkflowStatus.PENDING.equals(status) || 
               WorkflowStatus.IN_PROGRESS.equals(status);
    }
    
    /**
     * Check if workflow is approved
     */
    public boolean isApproved() {
        return WorkflowStatus.APPROVED.equals(status);
    }
    
    /**
     * Check if workflow is rejected
     */
    public boolean isRejected() {
        return WorkflowStatus.REJECTED.equals(status);
    }
    
    /**
     * Check if workflow is expired
     */
    public boolean isExpired() {
        return WorkflowStatus.EXPIRED.equals(status) ||
               (expiresAt != null && expiresAt.isBefore(Instant.now()));
    }
    
    /**
     * Check if workflow needs more approvals
     */
    public boolean needsMoreApprovals() {
        return isPending() && receivedApprovals < requiredApprovals;
    }
    
    /**
     * Check if workflow has sufficient approvals
     */
    public boolean hasSufficientApprovals() {
        return receivedApprovals >= requiredApprovals;
    }
    
    /**
     * Get remaining approvals needed
     */
    public int getRemainingApprovals() {
        return Math.max(0, requiredApprovals - receivedApprovals);
    }
    
    /**
     * Get approval progress percentage
     */
    public double getApprovalProgress() {
        if (requiredApprovals == 0) return 100.0;
        return (double) receivedApprovals / requiredApprovals * 100.0;
    }
    
    /**
     * Add approval
     */
    public void addApproval() {
        if (isPending()) {
            this.receivedApprovals++;
            this.status = WorkflowStatus.IN_PROGRESS;
            
            if (hasSufficientApprovals()) {
                this.status = WorkflowStatus.APPROVED;
                this.completedAt = Instant.now();
            }
        }
    }
    
    /**
     * Reject workflow
     */
    public void reject(String rejectedByUserId, String rejectionReason) {
        this.status = WorkflowStatus.REJECTED;
        this.completedAt = Instant.now();
        this.completedBy = rejectedByUserId;
        this.completionNotes = rejectionReason;
    }
    
    /**
     * Approve workflow
     */
    public void approve(String approvedByUserId, String approvalNotes) {
        this.status = WorkflowStatus.APPROVED;
        this.completedAt = Instant.now();
        this.completedBy = approvedByUserId;
        this.completionNotes = approvalNotes;
    }
    
    /**
     * Cancel workflow
     */
    public void cancel(String cancelledByUserId, String cancellationReason) {
        this.status = WorkflowStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.completedBy = cancelledByUserId;
        this.completionNotes = cancellationReason;
    }
    
    /**
     * Escalate workflow
     */
    public void escalate(String escalatedByUserId, String escalationReason) {
        this.status = WorkflowStatus.ESCALATED;
        this.priority = Priority.HIGH;
        this.completionNotes = "Escalated: " + escalationReason;
        
        // Extend expiry by 24 hours for escalated workflows
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
    }
    
    /**
     * Mark as expired
     */
    public void markExpired() {
        this.status = WorkflowStatus.EXPIRED;
        this.completedAt = Instant.now();
        this.completionNotes = "Workflow expired without sufficient approvals";
    }
    
    /**
     * Get user ID from transaction
     */
    public String getUserId() {
        return transaction != null && transaction.getUser() != null ? 
               transaction.getUser().getId() : null;
    }
    
    /**
     * Get transaction ID
     */
    public String getTransactionId() {
        return transaction != null ? transaction.getId() : null;
    }
    
    /**
     * Get display description
     */
    public String getDisplayDescription() {
        return approvalType.name().replace("_", " ") + 
               " for " + amount + 
               " (Transaction: " + getTransactionId() + ")";
    }
    
    /**
     * Create dual approval workflow
     */
    public static ApprovalWorkflow createDualApproval(Transaction transaction, 
                                                    String initiatedBy, 
                                                    String reason) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setTransaction(transaction);
        workflow.setApprovalType(ApprovalType.DUAL_APPROVAL);
        workflow.setInitiatedBy(initiatedBy);
        workflow.setAmount(transaction.getAmount());
        workflow.setReason(reason);
        workflow.setRequiredApprovals(2);
        workflow.setExpiresAt(Instant.now().plusSeconds(48 * 60 * 60)); // 48 hours
        
        return workflow;
    }
    
    /**
     * Create triple approval workflow
     */
    public static ApprovalWorkflow createTripleApproval(Transaction transaction, 
                                                      String initiatedBy, 
                                                      String reason) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setTransaction(transaction);
        workflow.setApprovalType(ApprovalType.TRIPLE_APPROVAL);
        workflow.setInitiatedBy(initiatedBy);
        workflow.setAmount(transaction.getAmount());
        workflow.setReason(reason);
        workflow.setRequiredApprovals(3);
        workflow.setPriority(Priority.HIGH);
        workflow.setExpiresAt(Instant.now().plusSeconds(72 * 60 * 60)); // 72 hours
        
        return workflow;
    }
    
    /**
     * Create compliance review workflow
     */
    public static ApprovalWorkflow createComplianceReview(Transaction transaction, 
                                                        String initiatedBy, 
                                                        String reason) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setTransaction(transaction);
        workflow.setApprovalType(ApprovalType.COMPLIANCE_REVIEW);
        workflow.setInitiatedBy(initiatedBy);
        workflow.setAmount(transaction.getAmount());
        workflow.setReason(reason);
        workflow.setRequiredApprovals(1);
        workflow.setPriority(Priority.HIGH);
        workflow.setExpiresAt(Instant.now().plusSeconds(24 * 60 * 60)); // 24 hours
        
        return workflow;
    }
    
    @Override
    public String toString() {
        return "ApprovalWorkflow{" +
               "id='" + getId() + '\'' +
               ", transactionId='" + getTransactionId() + '\'' +
               ", approvalType=" + approvalType +
               ", status=" + status +
               ", amount=" + amount +
               ", requiredApprovals=" + requiredApprovals +
               ", receivedApprovals=" + receivedApprovals +
               '}';
    }
}
