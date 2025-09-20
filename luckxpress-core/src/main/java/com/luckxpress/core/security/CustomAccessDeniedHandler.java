package com.luckxpress.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Access Denied Handler
 * CRITICAL: Handle insufficient privilege attempts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requestPath = request.getRequestURI();
        String clientIp = getClientIp(request);
        String username = auth != null ? auth.getName() : "anonymous";
        String authorities = auth != null ? auth.getAuthorities().toString() : "none";
        
        // Log access denied attempt
        log.warn("Access denied: User {} with authorities {} attempted to access {} from IP {}",
                username, authorities, requestPath, clientIp);
        
        // Track in Sentry for potential security issues
        Sentry.captureMessage(
            "Access denied - insufficient privileges",
            scope -> {
                scope.setTag("username", username);
                scope.setTag("request_path", requestPath);
                scope.setTag("client_ip", clientIp);
                scope.setTag("user_authorities", authorities);
                scope.setTag("error_type", "access_denied");
                scope.setLevel(io.sentry.SentryLevel.WARNING);
            }
        );
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", 403);
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", "Insufficient privileges");
        errorResponse.put("path", requestPath);
        
        // Add context for admin endpoints
        if (requestPath.contains("/admin")) {
            errorResponse.put("details", "Administrator privileges required");
        } else if (requestPath.contains("/kyc") || requestPath.contains("/compliance")) {
            errorResponse.put("details", "Compliance officer privileges required");
        } else {
            errorResponse.put("details", "You don't have permission to access this resource");
        }
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
