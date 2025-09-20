package com.luckxpress.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.time.LocalDate;

/**
 * KYC Verification entity
 * CRITICAL: Tracks KYC compliance for withdrawals and regulatory requirements
 */
@Entity
@Table(name = "kyc_verifications", indexes = {
    @Index(name = "idx_kyc_user", columnList = "user_id"),
    @Index(name = "idx_kyc_status", columnList = "status"),
    @Index(name = "idx_kyc_type", columnList = "verification_type"),
    @Index(name = "idx_kyc_submitted", columnList = "submitted_at"),
    @Index(name = "idx_kyc_verified", columnList = "verified_at"),
    @Index(name = "idx_kyc_expires", columnList = "expires_at")
})
@Audited
@Getter
@Setter
public class KycVerification extends AuditableEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 30)
    private VerificationType verificationType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.PENDING;
    
    @NotNull
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Size(max = 26)
    @Column(name = "verified_by", length = 26)
    private String verifiedBy;
    
    @Size(max = 100)
    @Column(name = "document_type", length = 100)
    private String documentType;
    
    @Size(max = 100)
    @Column(name = "document_number", length = 100)
    private String documentNumber;
    
    @Column(name = "document_expiry_date")
    private LocalDate documentExpiryDate;
    
    @Size(max = 100)
    @Column(name = "issuing_country", length = 100)
    private String issuingCountry;
    
    @Size(max = 100)
    @Column(name = "issuing_state", length = 100)
    private String issuingState;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "first_name", nullable = false, length = 200)
    private String firstName;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "last_name", nullable = false, length = 200)
    private String lastName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Size(max = 200)
    @Column(name = "address_line1", length = 200)
    private String addressLine1;
    
    @Size(max = 200)
    @Column(name = "address_line2", length = 200)
    private String addressLine2;
    
    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;
    
    @Size(max = 10)
    @Column(name = "state_code", length = 10)
    private String stateCode;
    
    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Size(max = 100)
    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "document_front_url", length = 500)
    private String documentFrontUrl;
    
    @Column(name = "document_back_url", length = 500)
    private String documentBackUrl;
    
    @Column(name = "selfie_url", length = 500)
    private String selfieUrl;
    
    @Column(name = "proof_of_address_url", length = 500)
    private String proofOfAddressUrl;
    
    @Size(max = 100)
    @Column(name = "external_verification_id", length = 100)
    private String externalVerificationId;
    
    @Size(max = 100)
    @Column(name = "verification_provider", length = 100)
    private String verificationProvider;
    
    @Column(name = "provider_response", columnDefinition = "JSONB")
    private String providerResponse;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * KYC verification type enumeration
     */
    public enum VerificationType {
        BASIC_KYC,          // Basic identity verification
        ENHANCED_KYC,       // Enhanced due diligence
        DOCUMENT_UPLOAD,    // Document verification only
        BIOMETRIC,          // Biometric verification
        ADDRESS_PROOF,      // Proof of address
        BANK_VERIFICATION,  // Bank account verification
        PHONE_VERIFICATION, // Phone number verification
        EMAIL_VERIFICATION  // Email verification
    }
    
    /**
     * KYC verification status enumeration
     */
    public enum VerificationStatus {
        PENDING,        // Submitted, waiting for review
        IN_PROGRESS,    // Under review
        APPROVED,       // Verification successful
        REJECTED,       // Verification failed
        EXPIRED,        // Verification expired
        CANCELLED,      // Verification cancelled
        REQUIRES_MANUAL_REVIEW  // Needs manual review
    }
    
    /**
     * Check if verification is approved
     */
    public boolean isApproved() {
        return VerificationStatus.APPROVED.equals(status);
    }
    
    /**
     * Check if verification is pending
     */
    public boolean isPending() {
        return VerificationStatus.PENDING.equals(status) || 
               VerificationStatus.IN_PROGRESS.equals(status);
    }
    
    /**
     * Check if verification is rejected
     */
    public boolean isRejected() {
        return VerificationStatus.REJECTED.equals(status);
    }
    
    /**
     * Check if verification is expired
     */
    public boolean isExpired() {
        return VerificationStatus.EXPIRED.equals(status) ||
               (expiresAt != null && expiresAt.isBefore(Instant.now()));
    }
    
    /**
     * Check if verification needs manual review
     */
    public boolean needsManualReview() {
        return VerificationStatus.REQUIRES_MANUAL_REVIEW.equals(status);
    }
    
    /**
     * Check if this is enhanced KYC
     */
    public boolean isEnhancedKyc() {
        return VerificationType.ENHANCED_KYC.equals(verificationType);
    }
    
    /**
     * Get full name from verification
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Get full address
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null) address.append(addressLine1);
        if (addressLine2 != null) address.append(", ").append(addressLine2);
        if (city != null) address.append(", ").append(city);
        if (stateCode != null) address.append(", ").append(stateCode);
        if (postalCode != null) address.append(" ").append(postalCode);
        if (country != null) address.append(", ").append(country);
        return address.toString();
    }
    
    /**
     * Approve verification
     */
    public void approve(String approvedByUserId, String notes) {
        this.status = VerificationStatus.APPROVED;
        this.verifiedAt = Instant.now();
        this.verifiedBy = approvedByUserId;
        if (notes != null) {
            this.notes = notes;
        }
        
        // Set expiry for enhanced KYC (1 year)
        if (isEnhancedKyc()) {
            this.expiresAt = Instant.now().plusSeconds(365 * 24 * 60 * 60); // 1 year
        }
    }
    
    /**
     * Reject verification
     */
    public void reject(String rejectedByUserId, String reason) {
        this.status = VerificationStatus.REJECTED;
        this.verifiedBy = rejectedByUserId;
        this.rejectionReason = reason;
    }
    
    /**
     * Mark as requiring manual review
     */
    public void requireManualReview(String reason) {
        this.status = VerificationStatus.REQUIRES_MANUAL_REVIEW;
        this.notes = reason;
    }
    
    /**
     * Mark as expired
     */
    public void markExpired() {
        this.status = VerificationStatus.EXPIRED;
    }
    
    /**
     * Calculate age from date of birth
     */
    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    /**
     * Check if user is of legal age (21+)
     */
    public boolean isOfLegalAge() {
        Integer age = getAge();
        return age != null && age >= 21;
    }
    
    @Override
    public String toString() {
        return "KycVerification{" +
               "id='" + getId() + '\'' +
               ", userId='" + (user != null ? user.getId() : null) + '\'' +
               ", verificationType=" + verificationType +
               ", status=" + status +
               ", submittedAt=" + submittedAt +
               ", verifiedAt=" + verifiedAt +
               '}';
    }
}
