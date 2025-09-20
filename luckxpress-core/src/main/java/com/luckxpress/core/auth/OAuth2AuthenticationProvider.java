package com.luckxpress.core.auth;

import com.luckxpress.core.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OAuth2 Authentication Provider
 * CRITICAL: Handles OAuth2 JWT token authentication
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationProvider implements AuthenticationProvider {
    
    private final JwtDecoder jwtDecoder;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        
        if (authentication instanceof BearerTokenAuthenticationToken bearerToken) {
            return authenticateJwtToken(bearerToken.getToken());
        }
        
        throw new BadCredentialsException("Unsupported authentication type");
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
    /**
     * Authenticate JWT token from OAuth2 provider
     */
    private Authentication authenticateJwtToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            UserPrincipal userPrincipal = createUserPrincipalFromJwt(jwt);
            
            return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                token,
                userPrincipal.getAuthorities()
            );
            
        } catch (JwtException ex) {
            log.error("Failed to decode JWT token", ex);
            throw new BadCredentialsException("Invalid JWT token", ex);
        }
    }
    
    /**
     * Create UserPrincipal from OAuth2 JWT claims
     */
    private UserPrincipal createUserPrincipalFromJwt(Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        
        // Extract roles from JWT (format may vary by OAuth2 provider)
        List<String> roles = extractRolesFromJwt(jwt);
        
        // Extract custom claims
        String stateCode = jwt.getClaimAsString("state_code");
        Boolean kycVerified = jwt.getClaimAsBoolean("kyc_verified");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        
        return UserPrincipal.builder()
            .userId(userId)
            .username(username != null ? username : email)
            .email(email)
            .stateCode(stateCode)
            .roles(roles)
            .kycVerified(kycVerified != null ? kycVerified : false)
            .enabled(emailVerified != null ? emailVerified : true)
            .accountLocked(false)
            .accountExpired(false)
            .credentialsExpired(false)
            .build();
    }
    
    /**
     * Extract roles from JWT token
     * Handles different OAuth2 provider formats
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromJwt(Jwt jwt) {
        // Try different claim names used by various OAuth2 providers
        
        // Keycloak format
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map) {
            Object roles = ((java.util.Map<String, Object>) realmAccess).get("roles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        }
        
        // Auth0 format
        Object auth0Roles = jwt.getClaim("https://luckxpress.com/roles");
        if (auth0Roles instanceof List) {
            return (List<String>) auth0Roles;
        }
        
        // Standard roles claim
        Object standardRoles = jwt.getClaim("roles");
        if (standardRoles instanceof List) {
            return (List<String>) standardRoles;
        }
        
        // Groups claim (some providers use this)
        Object groups = jwt.getClaim("groups");
        if (groups instanceof List) {
            return (List<String>) groups;
        }
        
        // Default role for authenticated users
        return List.of("USER");
    }
    
    /**
     * Validate JWT token claims for LuckXpress requirements
     */
    private void validateJwtClaims(Jwt jwt) throws AuthenticationException {
        // Validate required claims
        if (jwt.getClaimAsString("sub") == null) {
            throw new BadCredentialsException("JWT token missing subject claim");
        }
        
        if (jwt.getClaimAsString("email") == null) {
            throw new BadCredentialsException("JWT token missing email claim");
        }
        
        // Validate token is not expired
        if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new BadCredentialsException("JWT token is expired");
        }
        
        // Validate issuer if configured
        String expectedIssuer = "https://auth.luckxpress.com"; // Configure this
        if (jwt.getIssuer() != null && !expectedIssuer.equals(jwt.getIssuer().toString())) {
            throw new BadCredentialsException("JWT token from untrusted issuer");
        }
    }
}
