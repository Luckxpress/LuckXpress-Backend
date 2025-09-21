package com.luckxpress.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Marks a method as idempotent operation
 * CRITICAL: Ensures duplicate operations are safe
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentOperation {
    
    /**
     * Time to live for idempotency key
     */
    long ttl() default 1;
    
    /**
     * Time unit for TTL
     */
    TimeUnit unit() default TimeUnit.HOURS;
    
    /**
     * Key generator strategy
     */
    String keyGenerator() default "defaultIdempotencyKeyGenerator";
}
