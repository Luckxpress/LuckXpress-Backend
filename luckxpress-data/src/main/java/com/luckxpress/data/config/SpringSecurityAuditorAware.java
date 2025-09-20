package com.luckxpress.data.config;

import com.luckxpress.core.security.SecurityContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring Security Auditor Aware
 * CRITICAL: Provides current user ID for JPA auditing
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            String currentUserId = SecurityContext.getCurrentUserId();
            return Optional.ofNullable(currentUserId);
        } catch (Exception e) {
            // Fallback to SYSTEM if security context is not available
            return Optional.of("SYSTEM");
        }
    }
}
