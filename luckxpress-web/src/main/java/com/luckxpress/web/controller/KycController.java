package com.luckxpress.web.controller;

import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.KycVerification;
import com.luckxpress.service.KycService;
import com.luckxpress.web.dto.KycReviewRequestDto;
import com.luckxpress.web.dto.KycSubmissionDto;
import com.luckxpress.web.dto.KycVerificationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * KYC Controller
 * CRITICAL: Provides KYC verification REST endpoints with compliance validation
 */
@Slf4j
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "KYC Management", description = "KYC verification and compliance operations")
public class KycController {
    
    private final KycService kycService;
    
    /**
     * Submit KYC verification
     */
    @PostMapping("/submit")
    @Operation(
        summary = "Submit KYC verification",
        description = "Submits KYC verification documents and information"
    )
    @ApiResponse(responseCode = "201", description = "KYC verification submitted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid KYC submission data")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "409", description = "KYC already submitted or approved")
    public ResponseEntity<KycVerificationResponseDto> submitKycVerification(
            @Valid @RequestBody KycSubmissionDto kycSubmission,
            HttpServletRequest request) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.info("KYC verification submission: userId={}, type={}", 
                currentUserId, kycSubmission.getVerificationType());
        
        // Create service request
        KycService.KycVerificationRequest serviceRequest = new KycService.KycVerificationRequest();
        serviceRequest.setVerificationType(kycSubmission.getVerificationType());
        serviceRequest.setFirstName(kycSubmission.getFirstName());
        serviceRequest.setLastName(kycSubmission.getLastName());
        serviceRequest.setDateOfBirth(kycSubmission.getDateOfBirth());
        serviceRequest.setAddressLine1(kycSubmission.getAddressLine1());
        serviceRequest.setAddressLine2(kycSubmission.getAddressLine2());
        serviceRequest.setCity(kycSubmission.getCity());
        serviceRequest.setStateCode(kycSubmission.getStateCode());
        serviceRequest.setPostalCode(kycSubmission.getPostalCode());
        serviceRequest.setCountry(kycSubmission.getCountry());
        serviceRequest.setDocumentType(kycSubmission.getDocumentType());
        serviceRequest.setDocumentNumber(kycSubmission.getDocumentNumber());
        serviceRequest.setDocumentExpiryDate(kycSubmission.getDocumentExpiryDate());
        serviceRequest.setIssuingCountry(kycSubmission.getIssuingCountry());
        serviceRequest.setIssuingState(kycSubmission.getIssuingState());
        serviceRequest.setDocumentFrontUrl(kycSubmission.getDocumentFrontUrl());
        serviceRequest.setDocumentBackUrl(kycSubmission.getDocumentBackUrl());
        serviceRequest.setSelfieUrl(kycSubmission.getSelfieUrl());
        serviceRequest.setProofOfAddressUrl(kycSubmission.getProofOfAddressUrl());
        serviceRequest.setIpAddress(ipAddress);
        serviceRequest.setUserAgent(userAgent);
        
        KycVerification verification = kycService.submitKycVerification(currentUserId, serviceRequest);
        KycVerificationResponseDto response = KycVerificationResponseDto.fromVerification(verification);
        
        log.info("KYC verification submitted: verificationId={}, userId={}", 
                verification.getId(), currentUserId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get user's KYC verifications
     */
    @GetMapping
    @Operation(
        summary = "Get user KYC verifications",
        description = "Retrieves KYC verification history for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "KYC verifications retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<List<KycVerificationResponseDto>> getUserKycVerifications() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        List<KycVerification> verifications = kycService.findByUser(currentUserId);
        List<KycVerificationResponseDto> response = verifications.stream()
            .map(KycVerificationResponseDto::fromVerification)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get KYC verification status
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get KYC status",
        description = "Retrieves current KYC verification status for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "KYC status retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<KycStatusDto> getKycStatus() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        List<KycVerification> verifications = kycService.findByUser(currentUserId);
        
        KycStatusDto status = new KycStatusDto();
        status.setUserId(currentUserId);
        status.setHasSubmittedKyc(!verifications.isEmpty());
        
        if (!verifications.isEmpty()) {
            // Get latest verification
            KycVerification latest = verifications.get(0); // Assuming sorted by date
            status.setLatestStatus(latest.getStatus());
            status.setLatestVerificationType(latest.getVerificationType());
            status.setSubmittedAt(latest.getSubmittedAt());
            status.setVerifiedAt(latest.getVerifiedAt());
            
            if (latest.isApproved()) {
                status.setKycVerified(true);
                status.setVerificationLevel(latest.isEnhancedKyc() ? "ENHANCED" : "BASIC");
            }
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get KYC verification by ID (Compliance only)
     */
    @GetMapping("/{verificationId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get KYC verification by ID",
        description = "Retrieves KYC verification details by ID (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "KYC verification found")
    @ApiResponse(responseCode = "404", description = "KYC verification not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<KycVerificationResponseDto> getKycVerificationById(
            @Parameter(description = "KYC verification ID")
            @PathVariable String verificationId) {
        
        Optional<KycVerification> verification = kycService.findById(verificationId);
        if (verification.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        KycVerificationResponseDto response = KycVerificationResponseDto.fromVerification(verification.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Review KYC verification (Compliance only)
     */
    @PostMapping("/{verificationId}/review")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @Operation(
        summary = "Review KYC verification",
        description = "Reviews and makes decision on KYC verification (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "KYC verification reviewed successfully")
    @ApiResponse(responseCode = "404", description = "KYC verification not found")
    @ApiResponse(responseCode = "400", description = "Invalid review request")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<KycVerificationResponseDto> reviewKycVerification(
            @Parameter(description = "KYC verification ID")
            @PathVariable String verificationId,
            @Valid @RequestBody KycReviewRequestDto reviewRequest) {
        
        // Create service request
        KycService.KycReviewRequest serviceRequest = new KycService.KycReviewRequest();
        serviceRequest.setDecision(reviewRequest.getDecision());
        serviceRequest.setReason(reviewRequest.getReason());
        serviceRequest.setNotes(reviewRequest.getNotes());
        
        KycVerification verification = kycService.reviewKycVerification(verificationId, serviceRequest);
        KycVerificationResponseDto response = KycVerificationResponseDto.fromVerification(verification);
        
        log.info("KYC verification reviewed: verificationId={}, decision={}, reviewedBy={}", 
                verificationId, reviewRequest.getDecision(), SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get pending KYC verifications (Compliance only)
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get pending KYC verifications",
        description = "Retrieves all pending KYC verifications for review (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Pending KYC verifications retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<KycVerificationResponseDto>> getPendingKycVerifications() {
        List<KycVerification> verifications = kycService.findPendingVerifications();
        List<KycVerificationResponseDto> response = verifications.stream()
            .map(KycVerificationResponseDto::fromVerification)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get KYC requirements
     */
    @GetMapping("/requirements")
    @Operation(
        summary = "Get KYC requirements",
        description = "Retrieves KYC requirements and document specifications"
    )
    @ApiResponse(responseCode = "200", description = "KYC requirements retrieved")
    public ResponseEntity<KycRequirementsDto> getKycRequirements() {
        KycRequirementsDto requirements = new KycRequirementsDto();
        requirements.setMinAge(21);
        requirements.setRequiredDocuments(List.of(
            "Government-issued photo ID (Driver's License, Passport, State ID)",
            "Selfie photo",
            "Proof of address (for Enhanced KYC)"
        ));
        requirements.setAcceptedDocumentTypes(List.of(
            "DRIVERS_LICENSE", "PASSPORT", "STATE_ID", "MILITARY_ID"
        ));
        requirements.setSupportedStates(List.of(
            "All US states except: ID, WA, MT, NV"
        ));
        requirements.setProcessingTime("1-3 business days");
        requirements.setEnhancedKycThreshold("$2,500");
        
        return ResponseEntity.ok(requirements);
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * KYC Status DTO
     */
    public static class KycStatusDto {
        private String userId;
        private boolean hasSubmittedKyc;
        private boolean kycVerified;
        private String verificationLevel;
        private KycVerification.VerificationStatus latestStatus;
        private KycVerification.VerificationType latestVerificationType;
        private java.time.Instant submittedAt;
        private java.time.Instant verifiedAt;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public boolean isHasSubmittedKyc() { return hasSubmittedKyc; }
        public void setHasSubmittedKyc(boolean hasSubmittedKyc) { this.hasSubmittedKyc = hasSubmittedKyc; }
        
        public boolean isKycVerified() { return kycVerified; }
        public void setKycVerified(boolean kycVerified) { this.kycVerified = kycVerified; }
        
        public String getVerificationLevel() { return verificationLevel; }
        public void setVerificationLevel(String verificationLevel) { this.verificationLevel = verificationLevel; }
        
        public KycVerification.VerificationStatus getLatestStatus() { return latestStatus; }
        public void setLatestStatus(KycVerification.VerificationStatus latestStatus) { this.latestStatus = latestStatus; }
        
        public KycVerification.VerificationType getLatestVerificationType() { return latestVerificationType; }
        public void setLatestVerificationType(KycVerification.VerificationType latestVerificationType) { this.latestVerificationType = latestVerificationType; }
        
        public java.time.Instant getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(java.time.Instant submittedAt) { this.submittedAt = submittedAt; }
        
        public java.time.Instant getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(java.time.Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    }
    
    /**
     * KYC Requirements DTO
     */
    public static class KycRequirementsDto {
        private int minAge;
        private List<String> requiredDocuments;
        private List<String> acceptedDocumentTypes;
        private List<String> supportedStates;
        private String processingTime;
        private String enhancedKycThreshold;
        
        // Getters and setters
        public int getMinAge() { return minAge; }
        public void setMinAge(int minAge) { this.minAge = minAge; }
        
        public List<String> getRequiredDocuments() { return requiredDocuments; }
        public void setRequiredDocuments(List<String> requiredDocuments) { this.requiredDocuments = requiredDocuments; }
        
        public List<String> getAcceptedDocumentTypes() { return acceptedDocumentTypes; }
        public void setAcceptedDocumentTypes(List<String> acceptedDocumentTypes) { this.acceptedDocumentTypes = acceptedDocumentTypes; }
        
        public List<String> getSupportedStates() { return supportedStates; }
        public void setSupportedStates(List<String> supportedStates) { this.supportedStates = supportedStates; }
        
        public String getProcessingTime() { return processingTime; }
        public void setProcessingTime(String processingTime) { this.processingTime = processingTime; }
        
        public String getEnhancedKycThreshold() { return enhancedKycThreshold; }
        public void setEnhancedKycThreshold(String enhancedKycThreshold) { this.enhancedKycThreshold = enhancedKycThreshold; }
    }
}
