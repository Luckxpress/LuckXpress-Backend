package com.luckxpress.core.security;

import com.luckxpress.common.util.IdGenerator;
import io.sentry.Sentry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;

/**
 * Security audit filter for request tracking
 * CRITICAL: Log all authenticated requests for compliance
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Generate correlation ID
        String correlationId = IdGenerator.generateId("COR");
        MDC.put("correlationId", correlationId);
        MDC.put("requestPath", request.getRequestURI());
        MDC.put("requestMethod", request.getMethod());
        MDC.put("clientIp", getClientIp(request));
        MDC.put("userAgent", request.getHeader("User-Agent"));
        
        // Add to Sentry context
        Sentry.configureScope(scope -> {
            scope.setTag("correlation_id", correlationId);
            scope.setTag("request_path", request.getRequestURI());
            scope.setTag("http_method", request.getMethod());
        });
        
        // Set correlation ID in response
        response.setHeader("X-Correlation-Id", correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Check authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put("userId", auth.getName());
                MDC.put("userRoles", auth.getAuthorities().toString());
                
                // Audit log for sensitive endpoints
                if (isSensitiveEndpoint(request)) {
                    log.info(
                        "SECURITY_AUDIT: User {} accessing sensitive endpoint {} from IP {}",
                        auth.getName(),
                        request.getRequestURI(),
                        getClientIp(request)
                    );
                }
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("requestDuration", String.valueOf(duration));
            
            // Log slow requests
            if (duration > 1000) {
                log.warn("Slow request detected: {} ms for {}", 
                    duration, request.getRequestURI());
            }
            
            // Log request completion
            log.info(
                "Request completed: {} {} - Status: {} - Duration: {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration
            );
            
            // Clear MDC
            MDC.clear();
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Get first IP if multiple
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    private boolean isSensitiveEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/admin") ||
               uri.contains("/payment") ||
               uri.contains("/withdrawal") ||
               uri.contains("/wallet") ||
               uri.contains("/kyc");
    }
}
