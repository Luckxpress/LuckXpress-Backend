package com.luckxpress.data.entity;

import com.luckxpress.common.constants.StateRestriction;
import com.luckxpress.data.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity
 * CRITICAL: State must be validated for compliance
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_state", columnList = "state"),
    @Index(name = "idx_users_kyc_status", columnList = "kyc_status"),
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted = false")
public class User extends BaseEntity {
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "username", unique = true, length = 50)
    private String username;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "state", nullable = false, length = 2)
    private String state;
    
    @Column(name = "country", nullable = false, length = 2)
    private String country = "US";
    
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.NOT_STARTED;
    
    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;
    
    @Column(name = "kyc_verified_by")
    private String kycVerifiedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vip_tier", nullable = false, length = 20)
    private VipTier vipTier = VipTier.BRONZE;
    
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore = BigDecimal.ZERO;
    
    @Column(name = "lifetime_deposit", precision = 15, scale = 4)
    private BigDecimal lifetimeDeposit = BigDecimal.ZERO;
    
    @Column(name = "lifetime_withdrawal", precision = 15, scale = 4)
    private BigDecimal lifetimeWithdrawal = BigDecimal.ZERO;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;
    
    @Column(name = "is_self_excluded", nullable = false)
    private Boolean isSelfExcluded = false;
    
    @Column(name = "self_exclusion_until")
    private Instant selfExclusionUntil;
    
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    
    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;
    
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;
    
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private Instant lockedUntil;
    
    @Convert(converter = StringListConverter.class)
    @Column(name = "roles", columnDefinition = "TEXT")
    private List<String> roles = new ArrayList<>();
    
    @Column(name = "referral_code", unique = true, length = 20)
    private String referralCode;
    
    @Column(name = "referred_by", length = 32)
    private String referredBy;
    
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @Override
    protected String getEntityPrefix() {
        return "USR";
    }
    
    /**
     * Validate user state for sweeps operations
     */
    public void validateStateForSweeps() {
        StateRestriction.validateState(this.state);
    }
    
    /**
     * Check if user can withdraw
     */
    public boolean canWithdraw() {
        return kycStatus == KycStatus.VERIFIED && 
               !isLocked && 
               !isSelfExcluded &&
               !StateRestriction.isStateRestricted(state);
    }
    
    public enum KycStatus {
        NOT_STARTED,
        IN_PROGRESS,
        PENDING_REVIEW,
        VERIFIED,
        REJECTED,
        EXPIRED
    }
    
    public enum VipTier {
        BRONZE(BigDecimal.ZERO),
        SILVER(new BigDecimal("1000")),
        GOLD(new BigDecimal("5000")),
        PLATINUM(new BigDecimal("10000")),
        DIAMOND(new BigDecimal("50000"));
        
        private final BigDecimal threshold;
        
        VipTier(BigDecimal threshold) {
            this.threshold = threshold;
        }
        
        public BigDecimal getThreshold() {
            return threshold;
        }
    }
}
