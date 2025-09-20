package com.luckxpress.service;

import com.luckxpress.common.constants.StateRestriction;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.ComplianceAudit;
import com.luckxpress.data.entity.KycVerification;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * KYC Service
 * CRITICAL: Manages KYC verification process for compliance
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KycService {
    
    private final UserRepository userRepository;
    private final ComplianceService complianceService;
    private final AuditService auditService;
    
    /**
     * Submit KYC verification
     */
    @Transactional
    public KycVerification submitKycVerification(String userId, KycVerificationRequest request) {
        log.info("Submitting KYC verification: userId={}, type={}", userId, request.getVerificationType());
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Validate user can submit KYC
        validateKycSubmission(user, request);
        
        // Create KYC verification record
        KycVerification verification = new KycVerification();
        verification.setId(IdGenerator.generateId("KYC"));
        verification.setUser(user);
        verification.setVerificationType(request.getVerificationType());
        verification.setStatus(KycVerification.VerificationStatus.PENDING);
        verification.setSubmittedAt(Instant.now());
        
        // Set personal information
        verification.setFirstName(request.getFirstName());
        verification.setLastName(request.getLastName());
        verification.setDateOfBirth(request.getDateOfBirth());
        
        // Set address information
        verification.setAddressLine1(request.getAddressLine1());
        verification.setAddressLine2(request.getAddressLine2());
        verification.setCity(request.getCity());
        verification.setStateCode(request.getStateCode());
        verification.setPostalCode(request.getPostalCode());
        verification.setCountry(request.getCountry());
        
        // Set document information
        verification.setDocumentType(request.getDocumentType());
        verification.setDocumentNumber(request.getDocumentNumber());
        verification.setDocumentExpiryDate(request.getDocumentExpiryDate());
        verification.setIssuingCountry(request.getIssuingCountry());
        verification.setIssuingState(request.getIssuingState());
        
        // Set document URLs
        verification.setDocumentFrontUrl(request.getDocumentFrontUrl());
        verification.setDocumentBackUrl(request.getDocumentBackUrl());
        verification.setSelfieUrl(request.getSelfieUrl());
        verification.setProofOfAddressUrl(request.getProofOfAddressUrl());
        
        // Set verification metadata
        verification.setIpAddress(request.getIpAddress());
        verification.setUserAgent(request.getUserAgent());
        
        // Update user KYC status
        user.setKycStatus(User.KycStatus.IN_PROGRESS);
        userRepository.save(user);
        
        auditService.logKycSubmitted(user, verification);
        
        log.info("KYC verification submitted: verificationId={}, userId={}", verification.getId(), userId);
        
        return verification;
    }
    
    /**
     * Review KYC verification
     */
    @Transactional
    public KycVerification reviewKycVerification(String verificationId, KycReviewRequest request) {
        log.info("Reviewing KYC verification: verificationId={}, decision={}", verificationId, request.getDecision());
        
        KycVerification verification = findById(verificationId)
            .orElseThrow(() -> new IllegalArgumentException("KYC verification not found: " + verificationId));
        
        if (!verification.isPending()) {
            throw new IllegalStateException("KYC verification is not pending review");
        }
        
        String reviewerId = SecurityContext.getCurrentUserId();
        if (!SecurityContext.isComplianceOfficer()) {
            throw new IllegalStateException("Only compliance officers can review KYC verifications");
        }
        
        switch (request.getDecision()) {
            case APPROVE -> approveKycVerification(verification, reviewerId, request.getNotes());
            case REJECT -> rejectKycVerification(verification, reviewerId, request.getReason());
            case REQUIRE_MANUAL_REVIEW -> requireManualReview(verification, request.getReason());
        }
        
        return verification;
    }
    
    /**
     * Approve KYC verification
     */
    @Transactional
    public void approveKycVerification(KycVerification verification, String approvedBy, String notes) {
        log.info("Approving KYC verification: verificationId={}, approvedBy={}", verification.getId(), approvedBy);
        
        verification.approve(approvedBy, notes);
        
        // Update user KYC status
        User user = verification.getUser();
        user.verifyKyc();
        userRepository.save(user);
        
        auditService.logKycApproved(user, verification, approvedBy);
        
        log.info("KYC verification approved: verificationId={}, userId={}", verification.getId(), user.getId());
    }
    
    /**
     * Reject KYC verification
     */
    @Transactional
    public void rejectKycVerification(KycVerification verification, String rejectedBy, String reason) {
        log.info("Rejecting KYC verification: verificationId={}, rejectedBy={}, reason={}", 
                verification.getId(), rejectedBy, reason);
        
        verification.reject(rejectedBy, reason);
        
        // Update user KYC status
        User user = verification.getUser();
        user.setKycStatus(User.KycStatus.REJECTED);
        userRepository.save(user);
        
        auditService.logKycRejected(user, verification, rejectedBy, reason);
        
        log.info("KYC verification rejected: verificationId={}, userId={}", verification.getId(), user.getId());
    }
    
    /**
     * Require manual review for KYC verification
     */
    @Transactional
    public void requireManualReview(KycVerification verification, String reason) {
        log.info("Requiring manual review for KYC verification: verificationId={}, reason={}", 
                verification.getId(), reason);
        
        verification.requireManualReview(reason);
        
        auditService.logKycRequiresManualReview(verification.getUser(), verification, reason);
        
        log.info("KYC verification marked for manual review: verificationId={}", verification.getId());
    }
    
    /**
     * Find KYC verification by ID
     */
    public Optional<KycVerification> findById(String verificationId) {
        // This would use KycVerificationRepository when implemented
        return Optional.empty(); // Placeholder
    }
    
    /**
     * Find KYC verifications by user
     */
    public List<KycVerification> findByUser(String userId) {
        // This would use KycVerificationRepository when implemented
        return List.of(); // Placeholder
    }
    
    /**
     * Find pending KYC verifications
     */
    public List<KycVerification> findPendingVerifications() {
        // This would use KycVerificationRepository when implemented
        return List.of(); // Placeholder
    }
    
    /**
     * Check if user requires KYC for withdrawal
     */
    public boolean requiresKycForWithdrawal(User user, java.math.BigDecimal withdrawalAmount) {
        // Always require KYC for withdrawals
        if (!user.isKycVerified()) {
            return true;
        }
        
        // Check if enhanced KYC is required
        if (StateRestriction.requiresEnhancedKYC(user.getStateCode())) {
            return !hasValidEnhancedKyc(user);
        }
        
        return false;
    }
    
    /**
     * Check if user has valid enhanced KYC
     */
    public boolean hasValidEnhancedKyc(User user) {
        // This would check for enhanced KYC verification
        // Placeholder implementation
        return user.isKycVerified();
    }
    
    /**
     * Validate KYC submission
     */
    private void validateKycSubmission(User user, KycVerificationRequest request) {
        // Check if user already has approved KYC
        if (user.isKycVerified()) {
            throw new IllegalStateException("User already has approved KYC verification");
        }
        
        // Validate age (must be 21+)
        if (!request.isOfLegalAge()) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.UNDERAGE_USER_DETECTED,
                ComplianceAudit.Severity.HIGH,
                "Underage user KYC submission: age=" + request.getAge(),
                user
            );
            throw new IllegalArgumentException("Must be 21 or older for KYC verification");
        }
        
        // Validate state restrictions
        if (StateRestriction.isStateRestricted(request.getStateCode())) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.STATE_RESTRICTION_VIOLATION,
                ComplianceAudit.Severity.HIGH,
                "KYC submission from restricted state: " + request.getStateCode(),
                user
            );
            throw new IllegalArgumentException("KYC verification not available in state: " + request.getStateCode());
        }
        
        // Validate required documents
        validateRequiredDocuments(request);
        
        log.debug("KYC submission validation passed: userId={}", user.getId());
    }
    
    /**
     * Validate required documents
     */
    private void validateRequiredDocuments(KycVerificationRequest request) {
        if (request.getVerificationType() == KycVerification.VerificationType.BASIC_KYC) {
            if (request.getDocumentFrontUrl() == null || request.getDocumentFrontUrl().isEmpty()) {
                throw new IllegalArgumentException("Document front image is required");
            }
            if (request.getSelfieUrl() == null || request.getSelfieUrl().isEmpty()) {
                throw new IllegalArgumentException("Selfie image is required");
            }
        }
        
        if (request.getVerificationType() == KycVerification.VerificationType.ENHANCED_KYC) {
            if (request.getDocumentFrontUrl() == null || request.getDocumentFrontUrl().isEmpty()) {
                throw new IllegalArgumentException("Document front image is required");
            }
            if (request.getDocumentBackUrl() == null || request.getDocumentBackUrl().isEmpty()) {
                throw new IllegalArgumentException("Document back image is required");
            }
            if (request.getSelfieUrl() == null || request.getSelfieUrl().isEmpty()) {
                throw new IllegalArgumentException("Selfie image is required");
            }
            if (request.getProofOfAddressUrl() == null || request.getProofOfAddressUrl().isEmpty()) {
                throw new IllegalArgumentException("Proof of address is required for enhanced KYC");
            }
        }
    }
    
    /**
     * KYC Verification Request DTO
     */
    public static class KycVerificationRequest {
        private KycVerification.VerificationType verificationType;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String stateCode;
        private String postalCode;
        private String country;
        private String documentType;
        private String documentNumber;
        private LocalDate documentExpiryDate;
        private String issuingCountry;
        private String issuingState;
        private String documentFrontUrl;
        private String documentBackUrl;
        private String selfieUrl;
        private String proofOfAddressUrl;
        private String ipAddress;
        private String userAgent;
        
        // Getters and setters
        public KycVerification.VerificationType getVerificationType() { return verificationType; }
        public void setVerificationType(KycVerification.VerificationType verificationType) { this.verificationType = verificationType; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getStateCode() { return stateCode; }
        public void setStateCode(String stateCode) { this.stateCode = stateCode; }
        
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
        
        public LocalDate getDocumentExpiryDate() { return documentExpiryDate; }
        public void setDocumentExpiryDate(LocalDate documentExpiryDate) { this.documentExpiryDate = documentExpiryDate; }
        
        public String getIssuingCountry() { return issuingCountry; }
        public void setIssuingCountry(String issuingCountry) { this.issuingCountry = issuingCountry; }
        
        public String getIssuingState() { return issuingState; }
        public void setIssuingState(String issuingState) { this.issuingState = issuingState; }
        
        public String getDocumentFrontUrl() { return documentFrontUrl; }
        public void setDocumentFrontUrl(String documentFrontUrl) { this.documentFrontUrl = documentFrontUrl; }
        
        public String getDocumentBackUrl() { return documentBackUrl; }
        public void setDocumentBackUrl(String documentBackUrl) { this.documentBackUrl = documentBackUrl; }
        
        public String getSelfieUrl() { return selfieUrl; }
        public void setSelfieUrl(String selfieUrl) { this.selfieUrl = selfieUrl; }
        
        public String getProofOfAddressUrl() { return proofOfAddressUrl; }
        public void setProofOfAddressUrl(String proofOfAddressUrl) { this.proofOfAddressUrl = proofOfAddressUrl; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public boolean isOfLegalAge() {
            if (dateOfBirth == null) return false;
            return LocalDate.now().getYear() - dateOfBirth.getYear() >= 21;
        }
        
        public Integer getAge() {
            if (dateOfBirth == null) return null;
            return LocalDate.now().getYear() - dateOfBirth.getYear();
        }
    }
    
    /**
     * KYC Review Request DTO
     */
    public static class KycReviewRequest {
        private ReviewDecision decision;
        private String reason;
        private String notes;
        
        public enum ReviewDecision {
            APPROVE,
            REJECT,
            REQUIRE_MANUAL_REVIEW
        }
        
        // Getters and setters
        public ReviewDecision getDecision() { return decision; }
        public void setDecision(ReviewDecision decision) { this.decision = decision; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
