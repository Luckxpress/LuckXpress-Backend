package com.luckxpress.common.exception;

import lombok.Getter;

/**
 * Exception for compliance violations
 * CRITICAL: Log all compliance exceptions for audit
 */
@Getter
public class ComplianceException extends BaseException {
    
    public enum ComplianceType {
        STATE_RESTRICTION,
        KYC_REQUIRED,
        LIMIT_EXCEEDED,
        DUAL_APPROVAL_REQUIRED,
        AGE_VERIFICATION_FAILED,
        SELF_EXCLUSION_ACTIVE,
        SUSPICIOUS_ACTIVITY
    }
    
    private final ComplianceType type;
    private final String userId;
    
    public ComplianceException(ComplianceType type, String message, String userId) {
        super("COMPLIANCE_" + type.name(), message);
        this.type = type;
        this.userId = userId;
    }
    
    public static ComplianceException stateRestriction(String state, String userId) {
        return new ComplianceException(
            ComplianceType.STATE_RESTRICTION,
            String.format("Sweeps play is not available in %s", state),
            userId
        );
    }
    
    public static ComplianceException kycRequired(String userId) {
        return new ComplianceException(
            ComplianceType.KYC_REQUIRED,
            "KYC verification is required before this operation",
            userId
        );
    }
    
    public static ComplianceException limitExceeded(String limitType, String userId) {
        return new ComplianceException(
            ComplianceType.LIMIT_EXCEEDED,
            String.format("%s limit exceeded", limitType),
            userId
        );
    }
}
