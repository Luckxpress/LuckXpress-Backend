package com.luckxpress.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as requiring audit logging
 * CRITICAL: All financial operations must be audited
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAudit {
    
    /**
     * Audit event type
     */
    String eventType() default "";
    
    /**
     * Include method parameters in audit
     */
    boolean includeParams() default true;
    
    /**
     * Include return value in audit
     */
    boolean includeResult() default true;
    
    /**
     * Audit severity level
     */
    String level() default "INFO";
}
