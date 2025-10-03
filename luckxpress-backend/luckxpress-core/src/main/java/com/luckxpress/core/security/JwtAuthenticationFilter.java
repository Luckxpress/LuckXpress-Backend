package com.luckxpress.core.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that validates tokens for each request
 * Extends OncePerRequestFilter to ensure it's executed once per request
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwtToken = extractTokenFromRequest(request);
            
            if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtService.extractUsername(jwtToken);
                
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtService.validateToken(jwtToken, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );
                        
                        authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.debug("Authentication set for user: {}", username);
                    }
                }
            }
        } catch (ExpiredJwtException ex) {
            logger.error("JWT Token has expired: {}", ex.getMessage());
            request.setAttribute("expired", "JWT Token has expired");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT Token: {}", ex.getMessage());
            request.setAttribute("invalid", "Invalid JWT Token format");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT Token: {}", ex.getMessage());
            request.setAttribute("invalid", "Unsupported JWT Token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT Token compact of handler are invalid: {}", ex.getMessage());
            request.setAttribute("invalid", "JWT Token compact of handler are invalid");
        } catch (Exception ex) {
            logger.error("Cannot set user authentication: {}", ex.getMessage());
            request.setAttribute("invalid", "Authentication failed");
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the Authorization header
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Determines if the filter should not be applied to the current request
     * @param request HTTP request
     * @return true if the filter should be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator/health");
    }
}
