package com.luckxpress.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * CRITICAL: Handle unauthorized access attempts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        String requestPath = request.getRequestURI();
        String clientIp = getClientIp(request);
        
        // Log unauthorized access attempt
        log.warn("Unauthorized access attempt: {} from IP {} - {}",
                requestPath, clientIp, authException.getMessage());
        
        // Track in Sentry
        Sentry.captureMessage(
            "Unauthorized access attempt",
            scope -> {
                scope.setTag("request_path", requestPath);
                scope.setTag("client_ip", clientIp);
                scope.setTag("error_type", "authentication_failed");
                scope.setLevel(io.sentry.SentryLevel.WARNING);
            }
        );
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "Authentication required");
        errorResponse.put("path", requestPath);
        
        // Don't expose internal error details
        if (authException.getMessage().contains("JWT")) {
            errorResponse.put("details", "Invalid or expired token");
        } else {
            errorResponse.put("details", "Authentication credentials required");
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
