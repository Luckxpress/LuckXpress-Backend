package com.luckxpress.service;

import com.luckxpress.common.constants.ComplianceConstants;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.ApprovalWorkflow;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.data.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Approval Service
 * CRITICAL: Manages dual/triple approval workflows for high-value transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {
    
    private final AuditService auditService;
    
    /**
     * Find approval workflow by ID
     */
    public Optional<ApprovalWorkflow> findById(String workflowId) {
        // This would use ApprovalWorkflowRepository when implemented
        return Optional.empty(); // Placeholder
    }
    
    /**
     * Find approval workflows by transaction
     */
    public List<ApprovalWorkflow> findByTransaction(String transactionId) {
        // This would use ApprovalWorkflowRepository when implemented
        return List.of(); // Placeholder
    }
    
    /**
     * Find pending approval workflows
     */
    public List<ApprovalWorkflow> findPendingApprovals() {
        // This would use ApprovalWorkflowRepository when implemented
        return List.of(); // Placeholder
    }
    
    /**
     * Find approval workflows assigned to user
     */
    public List<ApprovalWorkflow> findAssignedApprovals(String userId) {
        // This would use ApprovalWorkflowRepository when implemented
        return List.of(); // Placeholder
    }
    
    /**
     * Create approval workflow for transaction
     */
    @Transactional
    public ApprovalWorkflow createApprovalWorkflow(Transaction transaction, String reason) {
        log.info("Creating approval workflow: transactionId={}, amount={}, reason={}", 
                transaction.getId(), transaction.getAmount(), reason);
        
        String initiatedBy = SecurityContext.getCurrentUserId();
        ApprovalWorkflow workflow = determineApprovalType(transaction, initiatedBy, reason);
        
        // Save workflow (would use repository)
        workflow.setId(IdGenerator.generateId("APW"));
        
        auditService.logApprovalWorkflowCreated(workflow, transaction);
        
        log.info("Approval workflow created: workflowId={}, type={}, requiredApprovals={}", 
                workflow.getId(), workflow.getApprovalType(), workflow.getRequiredApprovals());
        
        return workflow;
    }
    
    /**
     * Submit approval for workflow
     */
    @Transactional
    public ApprovalWorkflow submitApproval(String workflowId, String approvalNotes) {
        log.info("Submitting approval: workflowId={}, approver={}", workflowId, SecurityContext.getCurrentUserId());
        
        ApprovalWorkflow workflow = findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Approval workflow not found: " + workflowId));
        
        if (!workflow.isPending()) {
            throw new IllegalStateException("Workflow is not pending approval");
        }
        
        String approverId = SecurityContext.getCurrentUserId();
        
        // Validate approver has permission
        validateApprovalPermission(workflow, approverId);
        
        // Check if approver is the initiator (not allowed)
        if (workflow.getInitiatedBy().equals(approverId)) {
            throw new IllegalStateException("Cannot approve workflow you initiated");
        }
        
        // Add approval
        workflow.addApproval();
        
        // Check if workflow is now fully approved
        if (workflow.hasSufficientApprovals()) {
            workflow.approve(approverId, approvalNotes);
            
            auditService.logApprovalWorkflowCompleted(workflow, approverId);
            
            log.info("Approval workflow completed: workflowId={}, approver={}", workflowId, approverId);
        } else {
            auditService.logApprovalSubmitted(workflow, approverId, approvalNotes);
            
            log.info("Approval submitted: workflowId={}, approver={}, remaining={}", 
                    workflowId, approverId, workflow.getRemainingApprovals());
        }
        
        return workflow;
    }
    
    /**
     * Reject approval workflow
     */
    @Transactional
    public ApprovalWorkflow rejectApproval(String workflowId, String rejectionReason) {
        log.info("Rejecting approval: workflowId={}, rejector={}, reason={}", 
                workflowId, SecurityContext.getCurrentUserId(), rejectionReason);
        
        ApprovalWorkflow workflow = findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Approval workflow not found: " + workflowId));
        
        if (!workflow.isPending()) {
            throw new IllegalStateException("Workflow is not pending approval");
        }
        
        String rejectorId = SecurityContext.getCurrentUserId();
        
        // Validate rejector has permission
        validateApprovalPermission(workflow, rejectorId);
        
        workflow.reject(rejectorId, rejectionReason);
        
        auditService.logApprovalWorkflowRejected(workflow, rejectorId, rejectionReason);
        
        log.info("Approval workflow rejected: workflowId={}, rejector={}", workflowId, rejectorId);
        
        return workflow;
    }
    
    /**
     * Escalate approval workflow
     */
    @Transactional
    public ApprovalWorkflow escalateApproval(String workflowId, String escalationReason) {
        log.info("Escalating approval: workflowId={}, escalator={}, reason={}", 
                workflowId, SecurityContext.getCurrentUserId(), escalationReason);
        
        ApprovalWorkflow workflow = findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Approval workflow not found: " + workflowId));
        
        if (!workflow.isPending()) {
            throw new IllegalStateException("Workflow is not pending approval");
        }
        
        String escalatorId = SecurityContext.getCurrentUserId();
        
        // Only compliance officers and admins can escalate
        if (!SecurityContext.isComplianceOfficer()) {
            throw new IllegalStateException("Only compliance officers can escalate approvals");
        }
        
        workflow.escalate(escalatorId, escalationReason);
        
        auditService.logApprovalWorkflowEscalated(workflow, escalatorId, escalationReason);
        
        log.warn("Approval workflow escalated: workflowId={}, escalator={}", workflowId, escalatorId);
        
        return workflow;
    }
    
    /**
     * Cancel approval workflow
     */
    @Transactional
    public ApprovalWorkflow cancelApproval(String workflowId, String cancellationReason) {
        log.info("Cancelling approval: workflowId={}, canceller={}, reason={}", 
                workflowId, SecurityContext.getCurrentUserId(), cancellationReason);
        
        ApprovalWorkflow workflow = findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Approval workflow not found: " + workflowId));
        
        if (!workflow.isPending()) {
            throw new IllegalStateException("Workflow is not pending approval");
        }
        
        String cancellerId = SecurityContext.getCurrentUserId();
        
        // Only initiator or admin can cancel
        if (!workflow.getInitiatedBy().equals(cancellerId) && !SecurityContext.isAdmin()) {
            throw new IllegalStateException("Only workflow initiator or admin can cancel approval");
        }
        
        workflow.cancel(cancellerId, cancellationReason);
        
        auditService.logApprovalWorkflowCancelled(workflow, cancellerId, cancellationReason);
        
        log.info("Approval workflow cancelled: workflowId={}, canceller={}", workflowId, cancellerId);
        
        return workflow;
    }
    
    /**
     * Find expired approval workflows
     */
    public List<ApprovalWorkflow> findExpiredApprovals() {
        // This would use ApprovalWorkflowRepository when implemented
        return List.of(); // Placeholder
    }
    
    /**
     * Process expired approval workflows
     */
    @Transactional
    public void processExpiredApprovals() {
        log.info("Processing expired approval workflows");
        
        List<ApprovalWorkflow> expiredWorkflows = findExpiredApprovals();
        int processedCount = 0;
        
        for (ApprovalWorkflow workflow : expiredWorkflows) {
            try {
                if (workflow.isPending() && workflow.isExpired()) {
                    workflow.markExpired();
                    
                    auditService.logApprovalWorkflowExpired(workflow);
                    
                    processedCount++;
                    
                    log.warn("Approval workflow expired: workflowId={}, transactionId={}", 
                            workflow.getId(), workflow.getTransactionId());
                }
            } catch (Exception e) {
                log.error("Error processing expired workflow: workflowId={}", workflow.getId(), e);
            }
        }
        
        log.info("Processed expired approval workflows: count={}", processedCount);
    }
    
    /**
     * Determine approval type based on transaction
     */
    private ApprovalWorkflow determineApprovalType(Transaction transaction, String initiatedBy, String reason) {
        BigDecimal amount = transaction.getAmount();
        
        // Triple approval for very large amounts
        if (amount.compareTo(ComplianceConstants.TRIPLE_APPROVAL_THRESHOLD) >= 0) {
            return ApprovalWorkflow.createTripleApproval(transaction, initiatedBy, reason);
        }
        
        // Dual approval for large amounts
        if (amount.compareTo(ComplianceConstants.DUAL_APPROVAL_THRESHOLD) >= 0) {
            return ApprovalWorkflow.createDualApproval(transaction, initiatedBy, reason);
        }
        
        // Compliance review for suspicious transactions
        if (isSuspiciousTransaction(transaction)) {
            return ApprovalWorkflow.createComplianceReview(transaction, initiatedBy, reason);
        }
        
        // Default to dual approval
        return ApprovalWorkflow.createDualApproval(transaction, initiatedBy, reason);
    }
    
    /**
     * Check if transaction is suspicious
     */
    private boolean isSuspiciousTransaction(Transaction transaction) {
        // Check for suspicious patterns
        User user = transaction.getUser();
        
        // New user with large transaction
        if (user.getCreatedAt().isAfter(Instant.now().minusSeconds(7 * 24 * 60 * 60)) && // 7 days
            transaction.getAmount().compareTo(new BigDecimal("1000.0000")) >= 0) {
            return true;
        }
        
        // User from high-risk state
        if (user.getStateCode() != null && 
            (user.getStateCode().equals("NY") || user.getStateCode().equals("FL"))) {
            return transaction.getAmount().compareTo(new BigDecimal("500.0000")) >= 0;
        }
        
        return false;
    }
    
    /**
     * Validate approval permission
     */
    private void validateApprovalPermission(ApprovalWorkflow workflow, String approverId) {
        switch (workflow.getApprovalType()) {
            case DUAL_APPROVAL, TRIPLE_APPROVAL -> {
                // Any admin or compliance officer can approve
                if (!SecurityContext.isAdmin() && !SecurityContext.isComplianceOfficer()) {
                    throw new IllegalStateException("Insufficient permissions to approve this workflow");
                }
            }
            case COMPLIANCE_REVIEW -> {
                // Only compliance officers can approve compliance reviews
                if (!SecurityContext.isComplianceOfficer()) {
                    throw new IllegalStateException("Only compliance officers can approve compliance reviews");
                }
            }
            case FINANCE_APPROVAL -> {
                // Only finance managers and above can approve
                if (!SecurityContext.hasRole("FINANCE_MANAGER") && !SecurityContext.isAdmin()) {
                    throw new IllegalStateException("Only finance managers can approve finance workflows");
                }
            }
            case EXECUTIVE_APPROVAL -> {
                // Only executives can approve
                if (!SecurityContext.isAdmin()) {
                    throw new IllegalStateException("Only executives can approve executive workflows");
                }
            }
            default -> {
                // Default permission check
                if (!SecurityContext.isComplianceOfficer()) {
                    throw new IllegalStateException("Insufficient permissions to approve this workflow");
                }
            }
        }
    }
    
    /**
     * Get approval workflow statistics
     */
    public ApprovalStatistics getApprovalStatistics() {
        // This would aggregate data from ApprovalWorkflowRepository
        return new ApprovalStatistics(0, 0, 0, 0, 0); // Placeholder
    }
    
    /**
     * Approval Statistics DTO
     */
    public static class ApprovalStatistics {
        private final int pendingCount;
        private final int approvedCount;
        private final int rejectedCount;
        private final int expiredCount;
        private final int escalatedCount;
        
        public ApprovalStatistics(int pendingCount, int approvedCount, int rejectedCount, 
                                int expiredCount, int escalatedCount) {
            this.pendingCount = pendingCount;
            this.approvedCount = approvedCount;
            this.rejectedCount = rejectedCount;
            this.expiredCount = expiredCount;
            this.escalatedCount = escalatedCount;
        }
        
        // Getters
        public int getPendingCount() { return pendingCount; }
        public int getApprovedCount() { return approvedCount; }
        public int getRejectedCount() { return rejectedCount; }
        public int getExpiredCount() { return expiredCount; }
        public int getEscalatedCount() { return escalatedCount; }
        
        public int getTotalCount() {
            return pendingCount + approvedCount + rejectedCount + expiredCount + escalatedCount;
        }
        
        public double getApprovalRate() {
            int total = getTotalCount();
            return total > 0 ? (double) approvedCount / total * 100.0 : 0.0;
        }
    }
}
