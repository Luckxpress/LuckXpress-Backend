package com.luckxpress.web.controller.admin;

import com.luckxpress.common.annotation.RequiresDualApproval;
import com.luckxpress.service.compliance.ComplianceService;
import com.luckxpress.service.kyc.KycService;
import com.luckxpress.web.dto.request.KycDecisionRequest;
import com.luckxpress.web.dto.response.ComplianceReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * Admin compliance management controller
 * CRITICAL: All operations require ADMIN role and are audited
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/compliance")
@Tag(name = "Admin Compliance", description = "Compliance management operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminComplianceController {
    
    private final ComplianceService complianceService;
    private final KycService kycService;
    private final AuditService auditService;
    
    @GetMapping("/kyc/queue")
    @Operation(
        summary = "Get KYC verification queue",
        description = "Returns pending KYC cases ordered by priority"
    )
    @ApiResponse(responseCode = "200", description = "Queue retrieved")
    public ResponseEntity<List<KycCaseResponse>> getKycQueue(
            @RequestParam(defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Admin fetching KYC queue, status: {}", status);
        var cases = kycService.getQueue(KycStatus.valueOf(status), limit);
        return ResponseEntity.ok(cases);
    }
    
    @PostMapping("/kyc/{caseId}/decision")
    @Operation(
        summary = "Make KYC decision",
        description = "Approve or reject KYC verification"
    )
    @RequiresDualApproval(threshold = 10000) // High-value accounts need dual approval
    public ResponseEntity<KycDecisionResponse> makeKycDecision(
            @PathVariable String caseId,
            @Valid @RequestBody KycDecisionRequest request,
            @CurrentUser String adminId) {
        
        log.info("Admin {} making KYC decision for case: {}", adminId, caseId);
        
        var result = kycService.processDecision(
            caseId,
            request.getDecision(),
            request.getReason(),
            adminId
        );
        
        // Audit log
        auditService.logKycDecision(caseId, request.getDecision(), adminId);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/reports/daily")
    @Operation(
        summary = "Generate daily compliance report",
        description = "Comprehensive compliance metrics for the specified date"
    )
    public ResponseEntity<ComplianceReportResponse> getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Generating compliance report for date: {}", date);
        
        var report = complianceService.generateDailyReport(date);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/state-override")
    @Operation(
        summary = "Override state restriction",
        description = "Temporary override for testing (requires SUPER_ADMIN)"
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @RequiresDualApproval(threshold = 0) // Always requires dual approval
    public ResponseEntity<Void> overrideStateRestriction(
            @RequestParam String userId,
            @RequestParam String state,
            @RequestParam int durationMinutes,
            @CurrentUser String adminId) {
        
        log.warn("COMPLIANCE OVERRIDE: Admin {} overriding state restriction for user {} in state {} for {} minutes",
            adminId, userId, state, durationMinutes);
        
        complianceService.createTemporaryOverride(userId, state, durationMinutes, adminId);
        
        // Alert via Sentry
        Sentry.captureMessage(
            String.format("State restriction override: User %s, State %s, Admin %s", 
                userId, state, adminId),
            SentryLevel.WARNING
        );
        
        return ResponseEntity.accepted().build();
    }
    
    @GetMapping("/suspicious-activities")
    @Operation(
        summary = "Get suspicious activity alerts",
        description = "Returns recent suspicious activity flags"
    )
    public ResponseEntity<List<SuspiciousActivityResponse>> getSuspiciousActivities(
            @RequestParam(defaultValue = "7") int days) {
        
        var activities = complianceService.getSuspiciousActivities(days);
        return ResponseEntity.ok(activities);
    }
}
