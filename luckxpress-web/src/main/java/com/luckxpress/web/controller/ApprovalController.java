package com.luckxpress.web.controller;

import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.ApprovalWorkflow;
import com.luckxpress.service.ApprovalService;
import com.luckxpress.web.dto.ApprovalResponseDto;
import com.luckxpress.web.dto.ApprovalSubmissionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Approval Controller
 * CRITICAL: Provides approval workflow REST endpoints for dual/triple approval processes
 */
@Slf4j
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Approval Management", description = "Dual and triple approval workflow operations")
public class ApprovalController {
    
    private final ApprovalService approvalService;
    
    /**
     * Get pending approvals for current user
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Get pending approvals",
        description = "Retrieves pending approval workflows assigned to the current user"
    )
    @ApiResponse(responseCode = "200", description = "Pending approvals retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<ApprovalResponseDto>> getPendingApprovals() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        List<ApprovalWorkflow> workflows = approvalService.findAssignedApprovals(currentUserId);
        List<ApprovalResponseDto> response = workflows.stream()
            .filter(ApprovalWorkflow::isPending)
            .map(ApprovalResponseDto::fromWorkflow)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all pending approvals (Admin/Compliance only)
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get all pending approvals",
        description = "Retrieves all pending approval workflows (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "All pending approvals retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<ApprovalResponseDto>> getAllPendingApprovals() {
        List<ApprovalWorkflow> workflows = approvalService.findPendingApprovals();
        List<ApprovalResponseDto> response = workflows.stream()
            .map(ApprovalResponseDto::fromWorkflow)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get approval workflow by ID
     */
    @GetMapping("/{workflowId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Get approval workflow by ID",
        description = "Retrieves approval workflow details by ID"
    )
    @ApiResponse(responseCode = "200", description = "Approval workflow found")
    @ApiResponse(responseCode = "404", description = "Approval workflow not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<ApprovalResponseDto> getApprovalWorkflow(
            @Parameter(description = "Approval workflow ID")
            @PathVariable String workflowId) {
        
        Optional<ApprovalWorkflow> workflow = approvalService.findById(workflowId);
        if (workflow.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApprovalResponseDto response = ApprovalResponseDto.fromWorkflow(workflow.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Submit approval
     */
    @PostMapping("/{workflowId}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Submit approval",
        description = "Submits approval for the specified workflow"
    )
    @ApiResponse(responseCode = "200", description = "Approval submitted successfully")
    @ApiResponse(responseCode = "404", description = "Approval workflow not found")
    @ApiResponse(responseCode = "400", description = "Invalid approval request")
    @ApiResponse(responseCode = "403", description = "Access denied or insufficient permissions")
    public ResponseEntity<ApprovalResponseDto> submitApproval(
            @Parameter(description = "Approval workflow ID")
            @PathVariable String workflowId,
            @Valid @RequestBody ApprovalSubmissionDto approvalSubmission) {
        
        ApprovalWorkflow workflow = approvalService.submitApproval(workflowId, approvalSubmission.getNotes());
        ApprovalResponseDto response = ApprovalResponseDto.fromWorkflow(workflow);
        
        log.info("Approval submitted: workflowId={}, approver={}, remaining={}", 
                workflowId, SecurityContext.getCurrentUserId(), workflow.getRemainingApprovals());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reject approval
     */
    @PostMapping("/{workflowId}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Reject approval",
        description = "Rejects the specified approval workflow"
    )
    @ApiResponse(responseCode = "200", description = "Approval rejected successfully")
    @ApiResponse(responseCode = "404", description = "Approval workflow not found")
    @ApiResponse(responseCode = "400", description = "Invalid rejection request")
    @ApiResponse(responseCode = "403", description = "Access denied or insufficient permissions")
    public ResponseEntity<ApprovalResponseDto> rejectApproval(
            @Parameter(description = "Approval workflow ID")
            @PathVariable String workflowId,
            @Parameter(description = "Rejection reason")
            @RequestParam String rejectionReason) {
        
        ApprovalWorkflow workflow = approvalService.rejectApproval(workflowId, rejectionReason);
        ApprovalResponseDto response = ApprovalResponseDto.fromWorkflow(workflow);
        
        log.warn("Approval rejected: workflowId={}, rejector={}, reason={}", 
                workflowId, SecurityContext.getCurrentUserId(), rejectionReason);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Escalate approval
     */
    @PostMapping("/{workflowId}/escalate")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @Operation(
        summary = "Escalate approval",
        description = "Escalates approval workflow to higher authority (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Approval escalated successfully")
    @ApiResponse(responseCode = "404", description = "Approval workflow not found")
    @ApiResponse(responseCode = "400", description = "Invalid escalation request")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<ApprovalResponseDto> escalateApproval(
            @Parameter(description = "Approval workflow ID")
            @PathVariable String workflowId,
            @Parameter(description = "Escalation reason")
            @RequestParam String escalationReason) {
        
        ApprovalWorkflow workflow = approvalService.escalateApproval(workflowId, escalationReason);
        ApprovalResponseDto response = ApprovalResponseDto.fromWorkflow(workflow);
        
        log.warn("Approval escalated: workflowId={}, escalator={}, reason={}", 
                workflowId, SecurityContext.getCurrentUserId(), escalationReason);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel approval
     */
    @PostMapping("/{workflowId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cancel approval",
        description = "Cancels approval workflow (Admin only or workflow initiator)"
    )
    @ApiResponse(responseCode = "200", description = "Approval cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Approval workflow not found")
    @ApiResponse(responseCode = "400", description = "Invalid cancellation request")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<ApprovalResponseDto> cancelApproval(
            @Parameter(description = "Approval workflow ID")
            @PathVariable String workflowId,
            @Parameter(description = "Cancellation reason")
            @RequestParam String cancellationReason) {
        
        ApprovalWorkflow workflow = approvalService.cancelApproval(workflowId, cancellationReason);
        ApprovalResponseDto response = ApprovalResponseDto.fromWorkflow(workflow);
        
        log.info("Approval cancelled: workflowId={}, canceller={}, reason={}", 
                workflowId, SecurityContext.getCurrentUserId(), cancellationReason);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get approvals by transaction ID
     */
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get approvals by transaction ID",
        description = "Retrieves approval workflows for a specific transaction (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Transaction approvals retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<ApprovalResponseDto>> getApprovalsByTransaction(
            @Parameter(description = "Transaction ID")
            @PathVariable String transactionId) {
        
        List<ApprovalWorkflow> workflows = approvalService.findByTransaction(transactionId);
        List<ApprovalResponseDto> response = workflows.stream()
            .map(ApprovalResponseDto::fromWorkflow)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get approval statistics
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get approval statistics",
        description = "Retrieves approval workflow statistics (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Approval statistics retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<ApprovalService.ApprovalStatistics> getApprovalStatistics() {
        ApprovalService.ApprovalStatistics statistics = approvalService.getApprovalStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Process expired approvals
     */
    @PostMapping("/admin/process-expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Process expired approvals",
        description = "Processes and marks expired approval workflows (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Expired approvals processed")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> processExpiredApprovals() {
        approvalService.processExpiredApprovals();
        
        log.info("Expired approvals processed by: {}", SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get approval workflow history
     */
    @GetMapping("/admin/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get approval workflow history",
        description = "Retrieves historical approval workflows (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Approval history retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<ApprovalResponseDto>> getApprovalHistory(
            @Parameter(description = "Workflow status filter")
            @RequestParam(required = false) ApprovalWorkflow.WorkflowStatus status,
            @Parameter(description = "Approval type filter")
            @RequestParam(required = false) ApprovalWorkflow.ApprovalType approvalType,
            @Parameter(description = "Days back to search")
            @RequestParam(defaultValue = "30") int daysBack) {
        
        // This would be implemented with proper repository queries
        // For now, return empty list as placeholder
        List<ApprovalResponseDto> response = List.of();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get my approval activity
     */
    @GetMapping("/my-activity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Get my approval activity",
        description = "Retrieves approval activity for the current user"
    )
    @ApiResponse(responseCode = "200", description = "Approval activity retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<ApprovalActivityDto> getMyApprovalActivity() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        // This would aggregate approval activity for the user
        ApprovalActivityDto activity = new ApprovalActivityDto();
        activity.setUserId(currentUserId);
        activity.setPendingCount(0); // Placeholder
        activity.setApprovedCount(0); // Placeholder
        activity.setRejectedCount(0); // Placeholder
        activity.setEscalatedCount(0); // Placeholder
        
        return ResponseEntity.ok(activity);
    }
    
    /**
     * Approval Activity DTO
     */
    public static class ApprovalActivityDto {
        private String userId;
        private int pendingCount;
        private int approvedCount;
        private int rejectedCount;
        private int escalatedCount;
        private java.time.Instant lastActivity;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        
        public int getApprovedCount() { return approvedCount; }
        public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }
        
        public int getRejectedCount() { return rejectedCount; }
        public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
        
        public int getEscalatedCount() { return escalatedCount; }
        public void setEscalatedCount(int escalatedCount) { this.escalatedCount = escalatedCount; }
        
        public java.time.Instant getLastActivity() { return lastActivity; }
        public void setLastActivity(java.time.Instant lastActivity) { this.lastActivity = lastActivity; }
        
        public int getTotalCount() {
            return pendingCount + approvedCount + rejectedCount + escalatedCount;
        }
    }
}
