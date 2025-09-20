package com.luckxpress.core.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Security context management for LuckXpress
 * CRITICAL: Provides secure access to current user information
 */
@Getter
@Setter
public class SecurityContext {
    
    private static final String ANONYMOUS_USER = "ANONYMOUS";
    
    /**
     * Get current authenticated user ID
     * @return user ID or ANONYMOUS if not authenticated
     */
    public static String getCurrentUserId() {
        return getCurrentUserPrincipal()
            .map(UserPrincipal::getUserId)
            .orElse(ANONYMOUS_USER);
    }
    
    /**
     * Get current authenticated username
     * @return username or ANONYMOUS if not authenticated
     */
    public static String getCurrentUsername() {
        return getCurrentUserPrincipal()
            .map(UserPrincipal::getUsername)
            .orElse(ANONYMOUS_USER);
    }
    
    /**
     * Get current user's state code for compliance checks
     * @return state code or null if not available
     */
    public static String getCurrentUserState() {
        return getCurrentUserPrincipal()
            .map(principal -> principal.getStateCode())
            .orElse(null);
    }
    
    /**
     * Check if current user is authenticated
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && 
               auth.isAuthenticated() && 
               !ANONYMOUS_USER.equals(auth.getName());
    }
    
    /**
     * Check if current user has specific role
     * @param role the role to check
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        
        return auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
    
    /**
     * Check if current user has admin privileges
     * @return true if user is admin
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }
    
    /**
     * Check if current user has compliance officer privileges
     * @return true if user is compliance officer
     */
    public static boolean isComplianceOfficer() {
        return hasRole("COMPLIANCE_OFFICER") || isAdmin();
    }
    
    /**
     * Get current user principal
     * @return Optional containing UserPrincipal if authenticated
     */
    private static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) principal);
        }
        
        return Optional.empty();
    }
    
    /**
     * Clear security context (for testing or logout)
     */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
