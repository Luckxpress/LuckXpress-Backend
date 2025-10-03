package com.luckxpress.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles authentication exceptions and returns appropriate error responses
 * This component is invoked when an unauthenticated user tries to access a protected resource
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", determineErrorMessage(request, authException));
        errorDetails.put("path", request.getRequestURI());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
    
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        String expiredMsg = (String) request.getAttribute("expired");
        String invalidMsg = (String) request.getAttribute("invalid");
        
        if (expiredMsg != null) {
            return expiredMsg;
        } else if (invalidMsg != null) {
            return invalidMsg;
        } else if (authException != null && authException.getMessage() != null) {
            return authException.getMessage();
        } else {
            return "Full authentication is required to access this resource";
        }
    }
}
