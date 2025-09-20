package com.luckxpress.common.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User principal for authentication
 * CRITICAL: Contains user details for security context
 */
@Getter
@Builder
public class UserPrincipal implements UserDetails {
    
    private final String userId;
    private final String username;
    private final String email;
    private final String stateCode;
    private final List<String> roles;
    private final boolean kycVerified;
    private final boolean accountLocked;
    private final boolean accountExpired;
    private final boolean credentialsExpired;
    private final boolean enabled;
    private final Instant lastLoginAt;
    private final String sessionId;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        // Password is not stored in principal for security
        return null;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return !accountExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return !credentialsExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }
    
    /**
     * Check if user is compliance officer
     */
    public boolean isComplianceOfficer() {
        return hasRole("COMPLIANCE_OFFICER") || isAdmin();
    }
    
    /**
     * Check if user can perform withdrawals
     * COMPLIANCE: KYC verification required for withdrawals
     */
    public boolean canWithdraw() {
        return kycVerified && enabled && !accountLocked;
    }
    
    /**
     * Get display name for user
     */
    public String getDisplayName() {
        return username != null ? username : email;
    }
    
    /**
     * Create UserPrincipal from user entity data
     */
    public static UserPrincipal create(String userId, String username, String email, 
                                     String stateCode, List<String> roles, 
                                     boolean kycVerified, boolean enabled) {
        return UserPrincipal.builder()
            .userId(userId)
            .username(username)
            .email(email)
            .stateCode(stateCode)
            .roles(roles)
            .kycVerified(kycVerified)
            .accountLocked(false)
            .accountExpired(false)
            .credentialsExpired(false)
            .enabled(enabled)
            .lastLoginAt(Instant.now())
            .build();
    }
    
    /**
     * Create admin UserPrincipal for system operations
     */
    public static UserPrincipal createSystemAdmin() {
        return UserPrincipal.builder()
            .userId("SYSTEM")
            .username("SYSTEM")
            .email("system@luckxpress.com")
            .stateCode("SYS")
            .roles(List.of("SUPER_ADMIN"))
            .kycVerified(true)
            .accountLocked(false)
            .accountExpired(false)
            .credentialsExpired(false)
            .enabled(true)
            .lastLoginAt(Instant.now())
            .sessionId("SYSTEM_SESSION")
            .build();
    }
}
