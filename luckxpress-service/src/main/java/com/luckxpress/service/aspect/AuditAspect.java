package com.luckxpress.service.aspect;

import com.luckxpress.common.annotation.RequiresAudit;
import com.luckxpress.common.security.SecurityContext;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Audit aspect for financial operations
 * CRITICAL: Captures all audit-required operations for compliance
 */
@Aspect
@Component
@Slf4j
public class AuditAspect {
    
    private static final String AUDIT_CORRELATION_ID = "audit.correlation.id";
    
    @Before("@annotation(requiresAudit)")
    public void auditBefore(JoinPoint joinPoint, RequiresAudit requiresAudit) {
        String correlationId = generateCorrelationId();
        MDC.put(AUDIT_CORRELATION_ID, correlationId);
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();
        
        String eventType = requiresAudit.eventType().isEmpty() ? 
            methodName.toUpperCase() : requiresAudit.eventType();
        
        AuditEvent auditEvent = AuditEvent.builder()
            .correlationId(correlationId)
            .eventType(eventType)
            .className(className)
            .methodName(methodName)
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .status("STARTED")
            .build();
        
        if (requiresAudit.includeParams()) {
            auditEvent.setParameters(Arrays.toString(joinPoint.getArgs()));
        }
        
        logAuditEvent(auditEvent, requiresAudit.level());
        
        // Add to Sentry breadcrumbs
        Sentry.addBreadcrumb(
            String.format("Audit: %s.%s started", className, methodName),
            "audit"
        );
    }
    
    @AfterReturning(pointcut = "@annotation(requiresAudit)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, RequiresAudit requiresAudit, Object result) {
        String correlationId = MDC.get(AUDIT_CORRELATION_ID);
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();
        
        String eventType = requiresAudit.eventType().isEmpty() ? 
            methodName.toUpperCase() : requiresAudit.eventType();
        
        AuditEvent auditEvent = AuditEvent.builder()
            .correlationId(correlationId)
            .eventType(eventType)
            .className(className)
            .methodName(methodName)
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .status("COMPLETED")
            .build();
        
        if (requiresAudit.includeResult() && result != null) {
            auditEvent.setResult(result.toString());
        }
        
        logAuditEvent(auditEvent, requiresAudit.level());
        
        // Clean up MDC
        MDC.remove(AUDIT_CORRELATION_ID);
        
        // Add to Sentry breadcrumbs
        Sentry.addBreadcrumb(
            String.format("Audit: %s.%s completed", className, methodName),
            "audit"
        );
    }
    
    @AfterThrowing(pointcut = "@annotation(requiresAudit)", throwing = "exception")
    public void auditAfterThrowing(JoinPoint joinPoint, RequiresAudit requiresAudit, Exception exception) {
        String correlationId = MDC.get(AUDIT_CORRELATION_ID);
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();
        
        String eventType = requiresAudit.eventType().isEmpty() ? 
            methodName.toUpperCase() : requiresAudit.eventType();
        
        AuditEvent auditEvent = AuditEvent.builder()
            .correlationId(correlationId)
            .eventType(eventType)
            .className(className)
            .methodName(methodName)
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .status("FAILED")
            .errorMessage(exception.getMessage())
            .build();
        
        logAuditEvent(auditEvent, "ERROR");
        
        // Clean up MDC
        MDC.remove(AUDIT_CORRELATION_ID);
        
        // Capture exception in Sentry with audit context
        Sentry.captureException(exception, scope -> {
            scope.setTag("audit.event_type", eventType);
            scope.setTag("audit.correlation_id", correlationId);
            scope.setTag("audit.user_id", getCurrentUserId());
        });
    }
    
    private void logAuditEvent(AuditEvent event, String level) {
        String logMessage = String.format(
            "AUDIT [%s] %s.%s - User: %s, Status: %s, Correlation: %s",
            event.getEventType(),
            event.getClassName(),
            event.getMethodName(),
            event.getUserId(),
            event.getStatus(),
            event.getCorrelationId()
        );
        
        switch (level.toUpperCase()) {
            case "DEBUG":
                log.debug(logMessage);
                break;
            case "WARN":
                log.warn(logMessage);
                break;
            case "ERROR":
                log.error(logMessage);
                break;
            default:
                log.info(logMessage);
        }
        
        // In a production system, this would also send to a dedicated audit log system
        // e.g., Elasticsearch, Splunk, or a compliance-focused logging service
    }
    
    private String generateCorrelationId() {
        return "AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String getCurrentUserId() {
        return SecurityContext.getCurrentUserId().orElse("SYSTEM");
    }
    
    /**
     * Audit event data structure
     */
    @lombok.Builder
    @lombok.Data
    private static class AuditEvent {
        private String correlationId;
        private String eventType;
        private String className;
        private String methodName;
        private String userId;
        private Instant timestamp;
        private String status;
        private String parameters;
        private String result;
        private String errorMessage;
    }
}
