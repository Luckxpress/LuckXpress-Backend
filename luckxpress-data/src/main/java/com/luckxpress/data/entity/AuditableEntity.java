package com.luckxpress.data.entity;

import com.luckxpress.core.security.SecurityContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * Auditable entity with user tracking
 * CRITICAL: Tracks who created/modified records for compliance
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity extends BaseEntity {
    
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 26)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 26)
    private String updatedBy;
    
    @Column(name = "created_by_ip", length = 45)
    private String createdByIp;
    
    @Column(name = "updated_by_ip", length = 45)
    private String updatedByIp;
    
    /**
     * Set audit fields before persist
     */
    @PrePersist
    protected void onAuditCreate() {
        super.onCreate();
        
        String currentUserId = SecurityContext.getCurrentUserId();
        if (createdBy == null) {
            createdBy = currentUserId;
        }
        if (updatedBy == null) {
            updatedBy = currentUserId;
        }
        
        // Set IP addresses from request context if available
        // This would be populated by RequestContextFilter
        String currentIp = getCurrentUserIp();
        if (createdByIp == null) {
            createdByIp = currentIp;
        }
        if (updatedByIp == null) {
            updatedByIp = currentIp;
        }
    }
    
    /**
     * Set audit fields before update
     */
    @PreUpdate
    protected void onAuditUpdate() {
        super.onUpdate();
        
        String currentUserId = SecurityContext.getCurrentUserId();
        updatedBy = currentUserId;
        
        String currentIp = getCurrentUserIp();
        updatedByIp = currentIp;
    }
    
    /**
     * Get current user IP from request context
     * This would integrate with RequestContext from core module
     */
    private String getCurrentUserIp() {
        // This would be implemented to get IP from RequestContext
        // For now, return null - will be enhanced when RequestContext is integrated
        return null;
    }
    
    /**
     * Check if entity was created by system
     */
    public boolean isSystemCreated() {
        return "SYSTEM".equals(createdBy) || "ANONYMOUS".equals(createdBy);
    }
    
    /**
     * Check if entity was created by specific user
     */
    public boolean isCreatedBy(String userId) {
        return userId != null && userId.equals(createdBy);
    }
    
    /**
     * Check if entity was last modified by specific user
     */
    public boolean isLastModifiedBy(String userId) {
        return userId != null && userId.equals(updatedBy);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "id='" + getId() + '\'' +
               ", createdBy='" + createdBy + '\'' +
               ", updatedBy='" + updatedBy + '\'' +
               ", createdAt=" + getCreatedAt() +
               ", updatedAt=" + getUpdatedAt() +
               ", version=" + getVersion() +
               '}';
    }
}
