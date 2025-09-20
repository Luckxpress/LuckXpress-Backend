package com.luckxpress.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;

/**
 * Compliance Audit entity
 * CRITICAL: Records all compliance-related events for regulatory reporting
 */
@Entity
@Table(name = "compliance_audits", indexes = {
    @Index(name = "idx_compliance_user", columnList = "user_id"),
    @Index(name = "idx_compliance_event", columnList = "event_type"),
    @Index(name = "idx_compliance_severity", columnList = "severity"),
    @Index(name = "idx_compliance_status", columnList = "status"),
    @Index(name = "idx_compliance_occurred", columnList = "occurred_at"),
    @Index(name = "idx_compliance_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_compliance_ip", columnList = "ip_address"),
    @Index(name = "idx_compliance_resolved", columnList = "resolved_at")
})
@Audited
@Getter
@Setter
public class ComplianceAudit extends AuditableEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status = AuditStatus.OPEN;
    
    @NotBlank
    @Size(max = 1000)
    @Column(name = "description", nullable = false, length = 1000)
    private String description;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    
    @Size(max = 50)
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    @Size(max = 26)
    @Column(name = "entity_id", length = 26)
    private String entityId;
    
    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Size(max = 26)
    @Column(name = "session_id", length = 26)
    private String sessionId;
    
    @Size(max = 26)
    @Column(name = "request_id", length = 26)
    private String requestId;
    
    @Column(name = "risk_score")
    private Integer riskScore;
    
    @Size(max = 100)
    @Column(name = "risk_category", length = 100)
    private String riskCategory;
    
    @Column(name = "automated_action", length = 500)
    private String automatedAction;
    
    @Column(name = "requires_investigation", nullable = false)
    private Boolean requiresInvestigation = false;
    
    @Column(name = "investigated_at")
    private Instant investigatedAt;
    
    @Size(max = 26)
    @Column(name = "investigated_by", length = 26)
    private String investigatedBy;
    
    @Column(name = "investigation_notes", columnDefinition = "TEXT")
    private String investigationNotes;
    
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    
    @Size(max = 26)
    @Column(name = "resolved_by", length = 26)
    private String resolvedBy;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "regulatory_reported", nullable = false)
    private Boolean regulatoryReported = false;
    
    @Column(name = "reported_at")
    private Instant reportedAt;
    
    @Size(max = 100)
    @Column(name = "report_reference", length = 100)
    private String reportReference;
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    /**
     * Compliance event type enumeration
     */
    public enum EventType {
        // State Compliance
        STATE_RESTRICTION_VIOLATION,
        BLOCKED_STATE_ACCESS_ATTEMPT,
        
        // KYC Compliance
        KYC_VERIFICATION_FAILED,
        KYC_DOCUMENT_REJECTED,
        UNDERAGE_USER_DETECTED,
        IDENTITY_VERIFICATION_FAILED,
        
        // Transaction Compliance
        LARGE_TRANSACTION_DETECTED,
        SUSPICIOUS_TRANSACTION_PATTERN,
        RAPID_TRANSACTION_SEQUENCE,
        UNUSUAL_BETTING_PATTERN,
        
        // Money Laundering
        STRUCTURING_DETECTED,
        LAYERING_PATTERN_DETECTED,
        INTEGRATION_RISK_IDENTIFIED,
        
        // Withdrawal Compliance
        WITHDRAWAL_WITHOUT_KYC,
        EXCESSIVE_WITHDRAWAL_AMOUNT,
        FREQUENT_WITHDRAWAL_PATTERN,
        
        // Account Security
        MULTIPLE_FAILED_LOGINS,
        SUSPICIOUS_LOGIN_LOCATION,
        ACCOUNT_TAKEOVER_ATTEMPT,
        DEVICE_FINGERPRINT_MISMATCH,
        
        // Bonus Abuse
        BONUS_ABUSE_DETECTED,
        MULTIPLE_ACCOUNT_CREATION,
        COLLUSION_SUSPECTED,
        
        // System Security
        API_ABUSE_DETECTED,
        RATE_LIMIT_EXCEEDED,
        UNAUTHORIZED_ACCESS_ATTEMPT,
        
        // Regulatory
        SAR_THRESHOLD_EXCEEDED,
        CTR_THRESHOLD_EXCEEDED,
        OFAC_MATCH_DETECTED,
        PEP_MATCH_DETECTED,
        
        // Self-Exclusion
        SELF_EXCLUDED_USER_ACCESS,
        COOLING_OFF_VIOLATION,
        
        // Data Privacy
        GDPR_REQUEST_RECEIVED,
        DATA_BREACH_DETECTED,
        UNAUTHORIZED_DATA_ACCESS
    }
    
    /**
     * Audit severity enumeration
     */
    public enum Severity {
        LOW,        // Informational
        MEDIUM,     // Requires monitoring
        HIGH,       // Requires investigation
        CRITICAL    // Immediate action required
    }
    
    /**
     * Audit status enumeration
     */
    public enum AuditStatus {
        OPEN,           // New audit entry
        INVESTIGATING,  // Under investigation
        RESOLVED,       // Investigation complete
        CLOSED,         // Closed without action
        ESCALATED,      // Escalated to higher authority
        REPORTED        // Reported to regulators
    }
    
    /**
     * Check if audit is open
     */
    public boolean isOpen() {
        return AuditStatus.OPEN.equals(status) || 
               AuditStatus.INVESTIGATING.equals(status);
    }
    
    /**
     * Check if audit is resolved
     */
    public boolean isResolved() {
        return AuditStatus.RESOLVED.equals(status) || 
               AuditStatus.CLOSED.equals(status);
    }
    
    /**
     * Check if audit requires investigation
     */
    public boolean needsInvestigation() {
        return Boolean.TRUE.equals(requiresInvestigation) && 
               investigatedAt == null;
    }
    
    /**
     * Check if audit is high priority
     */
    public boolean isHighPriority() {
        return Severity.HIGH.equals(severity) || 
               Severity.CRITICAL.equals(severity);
    }
    
    /**
     * Check if audit needs regulatory reporting
     */
    public boolean needsRegulatoryReporting() {
        return isHighPriority() && 
               !Boolean.TRUE.equals(regulatoryReported) &&
               isResolved();
    }
    
    /**
     * Start investigation
     */
    public void startInvestigation(String investigatorId) {
        this.status = AuditStatus.INVESTIGATING;
        this.investigatedAt = Instant.now();
        this.investigatedBy = investigatorId;
    }
    
    /**
     * Resolve audit
     */
    public void resolve(String resolvedByUserId, String resolutionNotes) {
        this.status = AuditStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedByUserId;
        this.resolutionNotes = resolutionNotes;
    }
    
    /**
     * Close audit without action
     */
    public void close(String closedByUserId, String closureReason) {
        this.status = AuditStatus.CLOSED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = closedByUserId;
        this.resolutionNotes = "Closed: " + closureReason;
    }
    
    /**
     * Escalate audit
     */
    public void escalate(String escalatedByUserId, String escalationReason) {
        this.status = AuditStatus.ESCALATED;
        this.severity = Severity.CRITICAL;
        this.investigationNotes = "Escalated: " + escalationReason;
        this.requiresInvestigation = true;
    }
    
    /**
     * Mark as reported to regulators
     */
    public void markReported(String reportReference) {
        this.regulatoryReported = true;
        this.reportedAt = Instant.now();
        this.reportReference = reportReference;
        this.status = AuditStatus.REPORTED;
    }
    
    /**
     * Get user ID (handles null user)
     */
    public String getUserId() {
        return user != null ? user.getId() : null;
    }
    
    /**
     * Get username (handles null user)
     */
    public String getUsername() {
        return user != null ? user.getUsername() : "SYSTEM";
    }
    
    /**
     * Create compliance audit entry
     */
    public static ComplianceAudit create(EventType eventType, 
                                       Severity severity, 
                                       String description, 
                                       User user) {
        ComplianceAudit audit = new ComplianceAudit();
        audit.setEventType(eventType);
        audit.setSeverity(severity);
        audit.setDescription(description);
        audit.setUser(user);
        audit.setOccurredAt(Instant.now());
        
        // Set investigation requirement based on severity
        if (Severity.HIGH.equals(severity) || Severity.CRITICAL.equals(severity)) {
            audit.setRequiresInvestigation(true);
        }
        
        return audit;
    }
    
    /**
     * Create state restriction violation audit
     */
    public static ComplianceAudit createStateViolation(User user, 
                                                      String stateCode, 
                                                      String ipAddress) {
        ComplianceAudit audit = create(
            EventType.STATE_RESTRICTION_VIOLATION,
            Severity.HIGH,
            "User from restricted state " + stateCode + " attempted access",
            user
        );
        audit.setIpAddress(ipAddress);
        audit.setRiskCategory("STATE_COMPLIANCE");
        audit.setRequiresInvestigation(true);
        
        return audit;
    }
    
    /**
     * Create suspicious transaction audit
     */
    public static ComplianceAudit createSuspiciousTransaction(User user, 
                                                            String transactionId, 
                                                            String reason) {
        ComplianceAudit audit = create(
            EventType.SUSPICIOUS_TRANSACTION_PATTERN,
            Severity.HIGH,
            "Suspicious transaction pattern detected: " + reason,
            user
        );
        audit.setEntityType("Transaction");
        audit.setEntityId(transactionId);
        audit.setRiskCategory("AML");
        audit.setRequiresInvestigation(true);
        
        return audit;
    }
    
    @Override
    public String toString() {
        return "ComplianceAudit{" +
               "id='" + getId() + '\'' +
               ", eventType=" + eventType +
               ", severity=" + severity +
               ", status=" + status +
               ", userId='" + getUserId() + '\'' +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
