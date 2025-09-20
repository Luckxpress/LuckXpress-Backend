package com.luckxpress.core.mdc;

import com.luckxpress.common.util.IdGenerator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

/**
 * Request context for MDC (Mapped Diagnostic Context)
 * CRITICAL: Provides request tracing and audit trail
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestContext {
    
    // MDC Keys
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String USER_AGENT = "userAgent";
    public static final String REQUEST_URI = "requestUri";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String TIMESTAMP = "timestamp";
    public static final String CORRELATION_ID = "correlationId";
    
    /**
     * Initialize request context with basic information
     */
    public static void initialize() {
        String requestId = IdGenerator.generateId("REQ");
        MDC.put(REQUEST_ID, requestId);
        MDC.put(TIMESTAMP, Instant.now().toString());
        MDC.put(CORRELATION_ID, requestId); // Default correlation ID to request ID
    }
    
    /**
     * Set user information in MDC
     */
    public static void setUser(String userId, String sessionId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
        if (sessionId != null) {
            MDC.put(SESSION_ID, sessionId);
        }
    }
    
    /**
     * Set request information in MDC
     */
    public static void setRequestInfo(String uri, String method, String ipAddress, String userAgent) {
        if (uri != null) {
            MDC.put(REQUEST_URI, uri);
        }
        if (method != null) {
            MDC.put(HTTP_METHOD, method);
        }
        if (ipAddress != null) {
            MDC.put(IP_ADDRESS, ipAddress);
        }
        if (userAgent != null) {
            MDC.put(USER_AGENT, userAgent);
        }
    }
    
    /**
     * Set correlation ID for distributed tracing
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID, correlationId);
        }
    }
    
    /**
     * Get current request ID
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }
    
    /**
     * Get current user ID
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }
    
    /**
     * Get current correlation ID
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }
    
    /**
     * Get all MDC properties as Map
     */
    public static Map<String, String> getContextMap() {
        return MDC.getCopyOfContextMap();
    }
    
    /**
     * Set custom property in MDC
     */
    public static void setProperty(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }
    
    /**
     * Get property from MDC
     */
    public static String getProperty(String key) {
        return MDC.get(key);
    }
    
    /**
     * Clear all MDC properties
     */
    public static void clear() {
        MDC.clear();
    }
    
    /**
     * Create audit log entry with current context
     */
    public static String createAuditEntry(String action, String details) {
        return String.format(
            "AUDIT: [%s] User: %s, Action: %s, Details: %s, RequestId: %s, IP: %s",
            Instant.now(),
            getUserId(),
            action,
            details,
            getRequestId(),
            getProperty(IP_ADDRESS)
        );
    }
    
    /**
     * Create compliance log entry
     */
    public static String createComplianceEntry(String violation, String details) {
        return String.format(
            "COMPLIANCE: [%s] User: %s, Violation: %s, Details: %s, RequestId: %s, IP: %s",
            Instant.now(),
            getUserId(),
            violation,
            details,
            getRequestId(),
            getProperty(IP_ADDRESS)
        );
    }
}
