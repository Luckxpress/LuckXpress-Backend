package com.luckxpress.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Payment Idempotency Service - COMPLIANCE CRITICAL
 * 
 * COMPLIANCE RULE #6: Every payment operation MUST have idempotency key with Redis storage
 * 
 * This service ensures that payment operations are idempotent to prevent:
 * - Duplicate charges
 * - Double spending
 * - Race conditions in payment processing
 * - Financial discrepancies
 * 
 * All payment-related operations MUST use this service.
 */
@Service
public class PaymentIdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "luckxpress:idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24); // 24 hour TTL
    private static final Duration EXTENDED_TTL = Duration.ofDays(7);   // 7 day TTL for important operations

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public PaymentIdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "RedisTemplate cannot be null");
    }

    /**
     * Checks if an idempotency key has been used before
     * COMPLIANCE RULE #6: Must check before ALL payment operations
     * 
     * @param idempotencyKey the unique idempotency key
     * @return true if key has been used before
     */
    public boolean isKeyUsed(String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        
        String redisKey = buildRedisKey(idempotencyKey);
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    /**
     * Stores an idempotency key with payment result
     * COMPLIANCE RULE #6: Must store AFTER successful payment processing
     * 
     * @param idempotencyKey the unique idempotency key
     * @param paymentResult the result of the payment operation
     */
    public void storePaymentResult(String idempotencyKey, PaymentResult paymentResult) {
        validateIdempotencyKey(idempotencyKey);
        Objects.requireNonNull(paymentResult, "Payment result cannot be null");
        
        String redisKey = buildRedisKey(idempotencyKey);
        
        IdempotencyRecord record = new IdempotencyRecord(
            idempotencyKey,
            paymentResult,
            Instant.now()
        );
        
        // Store with appropriate TTL based on operation type
        Duration ttl = paymentResult.isHighValue() ? EXTENDED_TTL : DEFAULT_TTL;
        redisTemplate.opsForValue().set(redisKey, record, ttl);
    }

    /**
     * Retrieves the cached payment result for an idempotency key
     * COMPLIANCE RULE #6: Return cached result for duplicate requests
     * 
     * @param idempotencyKey the unique idempotency key
     * @return cached PaymentResult or null if not found
     */
    public PaymentResult getCachedResult(String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        
        String redisKey = buildRedisKey(idempotencyKey);
        IdempotencyRecord record = (IdempotencyRecord) redisTemplate.opsForValue().get(redisKey);
        
        return record != null ? record.getPaymentResult() : null;
    }

    /**
     * Attempts to acquire a lock for payment processing
     * Prevents concurrent processing of the same idempotency key
     * 
     * @param idempotencyKey the unique idempotency key
     * @param lockDuration how long to hold the lock
     * @return true if lock was acquired
     */
    public boolean acquirePaymentLock(String idempotencyKey, Duration lockDuration) {
        validateIdempotencyKey(idempotencyKey);
        Objects.requireNonNull(lockDuration, "Lock duration cannot be null");
        
        String lockKey = buildLockKey(idempotencyKey);
        String lockValue = "locked:" + Instant.now().toString();
        
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockDuration)
        );
    }

    /**
     * Releases a payment processing lock
     * 
     * @param idempotencyKey the unique idempotency key
     */
    public void releasePaymentLock(String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        
        String lockKey = buildLockKey(idempotencyKey);
        redisTemplate.delete(lockKey);
    }

    /**
     * Validates idempotency key format and requirements
     * 
     * @param idempotencyKey the key to validate
     * @throws IllegalArgumentException if key is invalid
     */
    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }
        
        String trimmedKey = idempotencyKey.trim();
        
        // Key should be at least 16 characters for security
        if (trimmedKey.length() < 16) {
            throw new IllegalArgumentException("Idempotency key must be at least 16 characters long");
        }
        
        // Key should not exceed reasonable length
        if (trimmedKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency key cannot exceed 255 characters");
        }
        
        // Key should only contain safe characters
        if (!trimmedKey.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Idempotency key can only contain alphanumeric characters, underscores, and hyphens");
        }
    }

    /**
     * Builds Redis key for idempotency storage
     * 
     * @param idempotencyKey the idempotency key
     * @return Redis key
     */
    private String buildRedisKey(String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + idempotencyKey.trim();
    }

    /**
     * Builds Redis key for payment processing locks
     * 
     * @param idempotencyKey the idempotency key
     * @return Redis lock key
     */
    private String buildLockKey(String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey.trim();
    }

    /**
     * Internal record for storing idempotency data in Redis
     */
    public static class IdempotencyRecord {
        private final String idempotencyKey;
        private final PaymentResult paymentResult;
        private final Instant createdAt;

        public IdempotencyRecord(String idempotencyKey, PaymentResult paymentResult, Instant createdAt) {
            this.idempotencyKey = idempotencyKey;
            this.paymentResult = paymentResult;
            this.createdAt = createdAt;
        }

        public String getIdempotencyKey() { return idempotencyKey; }
        public PaymentResult getPaymentResult() { return paymentResult; }
        public Instant getCreatedAt() { return createdAt; }
    }

    /**
     * Payment result structure for idempotency caching
     */
    public static class PaymentResult {
        private final String transactionId;
        private final String status;
        private final String amount;
        private final boolean highValue;
        private final Instant processedAt;

        public PaymentResult(String transactionId, String status, String amount, boolean highValue) {
            this.transactionId = transactionId;
            this.status = status;
            this.amount = amount;
            this.highValue = highValue;
            this.processedAt = Instant.now();
        }

        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getAmount() { return amount; }
        public boolean isHighValue() { return highValue; }
        public Instant getProcessedAt() { return processedAt; }
    }
}
