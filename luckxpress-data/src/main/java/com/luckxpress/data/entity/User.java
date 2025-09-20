package com.luckxpress.data.entity;

import com.luckxpress.common.validation.ValidState;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity
 * CRITICAL: Core user information with compliance tracking
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_state", columnList = "state_code"),
    @Index(name = "idx_user_status", columnList = "status"),
    @Index(name = "idx_user_kyc", columnList = "kyc_status")
})
@Audited
@Getter
@Setter
public class User extends AuditableEntity {
    
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank
    @Email
    @Size(max = 100)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @NotNull
    @ValidState
    @Size(min = 2, max = 2)
    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;
    
    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;
    
    @Size(max = 10)
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;
    
    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;
    
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    
    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;
    
    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;
    
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;
    
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;
    
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;
    
    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;
    
    @Column(name = "privacy_accepted_at")
    private Instant privacyAcceptedAt;
    
    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent = false;
    
    @Column(name = "self_exclusion_until")
    private Instant selfExclusionUntil;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 50)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * User status enumeration
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        BANNED,
        PENDING_VERIFICATION
    }
    
    /**
     * KYC status enumeration
     */
    public enum KycStatus {
        PENDING,
        IN_PROGRESS,
        APPROVED,
        REJECTED,
        EXPIRED,
        ENHANCED_REQUIRED
    }
    
    /**
     * User role enumeration
     */
    public enum UserRole {
        USER,
        VIP,
        ADMIN,
        SUPER_ADMIN,
        COMPLIANCE_OFFICER,
        CUSTOMER_SERVICE,
        FINANCE_MANAGER
    }
    
    /**
     * Check if user is active
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }
    
    /**
     * Check if user is KYC verified
     */
    public boolean isKycVerified() {
        return KycStatus.APPROVED.equals(kycStatus);
    }
    
    /**
     * Check if user can withdraw (KYC + active + not self-excluded)
     */
    public boolean canWithdraw() {
        return isActive() && 
               isKycVerified() && 
               !isAccountLocked() && 
               !isSelfExcluded();
    }
    
    /**
     * Check if account is locked
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && 
               accountLockedUntil.isAfter(Instant.now());
    }
    
    /**
     * Check if user is self-excluded
     */
    public boolean isSelfExcluded() {
        return selfExclusionUntil != null && 
               selfExclusionUntil.isAfter(Instant.now());
    }
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN) || hasRole(UserRole.SUPER_ADMIN);
    }
    
    /**
     * Check if user is compliance officer
     */
    public boolean isComplianceOfficer() {
        return hasRole(UserRole.COMPLIANCE_OFFICER) || isAdmin();
    }
    
    /**
     * Get full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
    
    /**
     * Get display name
     */
    public String getDisplayName() {
        String fullName = getFullName();
        return fullName.equals(username) ? username : fullName + " (" + username + ")";
    }
    
    /**
     * Record successful login
     */
    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ipAddress;
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }
    
    /**
     * Record failed login attempt
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        
        // Lock account after 5 failed attempts for 30 minutes
        if (this.failedLoginAttempts >= 5) {
            this.accountLockedUntil = Instant.now().plusSeconds(1800); // 30 minutes
        }
    }
    
    /**
     * Verify KYC
     */
    public void verifyKyc() {
        this.kycStatus = KycStatus.APPROVED;
        this.kycVerifiedAt = Instant.now();
    }
    
    /**
     * Set self-exclusion period
     */
    public void setSelfExclusion(Instant until) {
        this.selfExclusionUntil = until;
    }
}
