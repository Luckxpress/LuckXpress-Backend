package com.luckxpress.service.compliance;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Compliance validation result
 * CRITICAL: Tracks compliance status for regulatory reporting
 */
@Data
@Builder
public class ComplianceResult {
    
    private final boolean compliant;
    private final String violationCode;
    private final String violationMessage;
    private final String recommendedAction;
    
    @Builder.Default
    private final Instant checkedAt = Instant.now();
    
    /**
     * Create compliant result
     */
    public static ComplianceResult compliant() {
        return ComplianceResult.builder()
            .compliant(true)
            .build();
    }
    
    /**
     * Create violation result
     */
    public static ComplianceResult violation(String code, String message) {
        return ComplianceResult.builder()
            .compliant(false)
            .violationCode(code)
            .violationMessage(message)
            .build();
    }
    
    /**
     * Create violation result with recommended action
     */
    public static ComplianceResult violation(String code, String message, String action) {
        return ComplianceResult.builder()
            .compliant(false)
            .violationCode(code)
            .violationMessage(message)
            .recommendedAction(action)
            .build();
    }
    
    /**
     * Check if there's a compliance violation
     */
    public boolean hasViolation() {
        return !compliant;
    }
}
