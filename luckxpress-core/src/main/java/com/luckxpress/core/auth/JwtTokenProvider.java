package com.luckxpress.core.auth;

import com.luckxpress.core.security.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * JWT Token Provider
 * CRITICAL: Handles JWT token creation, validation, and parsing
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;
    private final String issuer;
    
    public JwtTokenProvider(
            @Value("${luckxpress.jwt.secret:luckxpress-super-secret-key-that-should-be-changed-in-production}") String secret,
            @Value("${luckxpress.jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
            @Value("${luckxpress.jwt.refresh-token-validity:86400}") long refreshTokenValidityInSeconds,
            @Value("${luckxpress.jwt.issuer:luckxpress}") String issuer) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
        this.issuer = issuer;
    }
    
    /**
     * Generate access token for user
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(accessTokenValidityInSeconds, ChronoUnit.SECONDS);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getUserId())
            .setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiryDate))
            .claim("username", userPrincipal.getUsername())
            .claim("email", userPrincipal.getEmail())
            .claim("roles", userPrincipal.getRoles())
            .claim("stateCode", userPrincipal.getStateCode())
            .claim("kycVerified", userPrincipal.isKycVerified())
            .claim("type", "access")
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getUserId())
            .setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiryDate))
            .claim("type", "refresh")
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Get UserPrincipal from JWT token
     */
    public UserPrincipal getUserPrincipalFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String email = claims.get("email", String.class);
            String stateCode = claims.get("stateCode", String.class);
            Boolean kycVerified = claims.get("kycVerified", Boolean.class);
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            
            return UserPrincipal.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .stateCode(stateCode)
                .roles(roles != null ? roles : List.of())
                .kycVerified(kycVerified != null ? kycVerified : false)
                .enabled(true)
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
                
        } catch (Exception ex) {
            log.error("Error parsing JWT token", ex);
            return null;
        }
    }
    
    /**
     * Get user ID from token
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return claims.getSubject();
        } catch (Exception ex) {
            log.error("Error extracting user ID from token", ex);
            return null;
        }
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature", ex);
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token", ex);
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token", ex);
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token", ex);
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty", ex);
        }
        
        return false;
    }
    
    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Get token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return claims.getExpiration();
        } catch (Exception ex) {
            log.error("Error extracting expiration date from token", ex);
            return null;
        }
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
}
