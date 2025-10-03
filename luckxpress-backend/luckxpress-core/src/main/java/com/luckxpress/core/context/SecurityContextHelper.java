package com.luckxpress.core.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper class for accessing security context information
 * Provides utility methods for retrieving current user details
 */
@Component
public class SecurityContextHelper {

    /**
     * Gets the current authenticated user's username
     * @return Optional containing username if authenticated, empty otherwise
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        } else if (principal instanceof String) {
            return Optional.of((String) principal);
        }
        
        return Optional.empty();
    }

    /**
     * Gets the current authenticated user details
     * @return Optional containing UserDetails if authenticated, empty otherwise
     */
    public Optional<UserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return Optional.of((UserDetails) principal);
        }
        
        return Optional.empty();
    }

    /**
     * Checks if the current user is authenticated
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Checks if the current user has a specific role
     * @param role the role to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String rolePrefix = "ROLE_";
        String roleName = role.startsWith(rolePrefix) ? role : rolePrefix + role;
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleName));
    }

    /**
     * Checks if the current user has any of the specified roles
     * @param roles the roles to check
     * @return true if user has any of the roles, false otherwise
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the current security context
     */
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
