package com.luckxpress.core.monitoring;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

/**
 * Sentry integration for compliance monitoring
 * CRITICAL: Track all compliance violations
 */
@Aspect
@Component
public class ComplianceSentryIntegration {
    
    @Around("@annotation(com.luckxpress.common.annotation.RequiresKYC)")
    public Object trackKYCCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setCategory("compliance.kyc");
        breadcrumb.setLevel(SentryLevel.INFO);
        breadcrumb.setMessage("KYC check initiated");
        Sentry.addBreadcrumb(breadcrumb);
        
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            Sentry.captureException(e, scope -> {
                scope.setTag("compliance.type", "kyc_failure");
                scope.setLevel(SentryLevel.ERROR);
                scope.setFingerprint(List.of("kyc-check-failed", e.getClass().getSimpleName()));
            });
            throw e;
        }
    }
    
    @Around("@annotation(com.luckxpress.common.annotation.RequiresDualApproval)")
    public Object trackDualApproval(ProceedingJoinPoint joinPoint) throws Throwable {
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setCategory("compliance.approval");
        breadcrumb.setLevel(SentryLevel.INFO);
        breadcrumb.setMessage("Dual approval check");
        Sentry.addBreadcrumb(breadcrumb);
        
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            Sentry.captureException(e, scope -> {
                scope.setTag("compliance.type", "dual_approval_required");
                scope.setLevel(SentryLevel.WARNING);
            });
            throw e;
        }
    }
    
    public static void trackStateRestriction(String state, String userId) {
        Sentry.captureMessage(
            String.format("State restriction triggered for %s", state),
            SentryLevel.WARNING,
            scope -> {
                scope.setTag("compliance.type", "state_restriction");
                scope.setTag("state", state);
                scope.setTag("user_id", userId);
                scope.setFingerprint(List.of("state-restriction", state));
            }
        );
    }
    
    public static void trackSuspiciousTransaction(String userId, BigDecimal amount, String reason) {
        Sentry.captureMessage(
            "Suspicious transaction detected",
            SentryLevel.ERROR,
            scope -> {
                scope.setTag("compliance.type", "suspicious_transaction");
                scope.setTag("user_id", userId);
                scope.setTag("amount", amount.toPlainString());
                scope.setTag("reason", reason);
                scope.setFingerprint(List.of("suspicious-transaction", reason));
            }
        );
    }
}
