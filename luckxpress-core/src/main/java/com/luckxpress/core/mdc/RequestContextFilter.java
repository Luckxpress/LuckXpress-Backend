package com.luckxpress.core.mdc;

import com.luckxpress.core.security.SecurityContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Request Context Filter for MDC setup
 * CRITICAL: Sets up request tracing and audit context
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String USER_AGENT = "User-Agent";
    private static final String X_CORRELATION_ID = "X-Correlation-ID";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Initialize request context
            RequestContext.initialize();
            
            // Set request information
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader(USER_AGENT);
            
            RequestContext.setRequestInfo(requestUri, method, ipAddress, userAgent);
            
            // Set correlation ID if provided
            String correlationId = request.getHeader(X_CORRELATION_ID);
            if (correlationId != null) {
                RequestContext.setCorrelationId(correlationId);
                response.setHeader(X_CORRELATION_ID, correlationId);
            } else {
                // Return the generated request ID as correlation ID
                response.setHeader(X_CORRELATION_ID, RequestContext.getRequestId());
            }
            
            log.debug("Request started: {} {} from {}", method, requestUri, ipAddress);
            
            // Process the request
            filterChain.doFilter(request, response);
            
            log.debug("Request completed: {} {} - Status: {}", method, requestUri, response.getStatus());
            
        } catch (Exception ex) {
            log.error("Error in request context filter", ex);
            throw ex;
        } finally {
            // Set user context after authentication (if available)
            if (SecurityContext.isAuthenticated()) {
                RequestContext.setUser(
                    SecurityContext.getCurrentUserId(),
                    null // Session ID would be set separately if available
                );
            }
            
            // Clear MDC after request processing
            RequestContext.clear();
        }
    }
    
    /**
     * Extract client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader(X_REAL_IP);
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Skip filter for static resources
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.equals("/favicon.ico") ||
               path.equals("/robots.txt");
    }
}
