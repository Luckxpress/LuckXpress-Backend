package com.luckxpress.data.repository;

import com.luckxpress.data.entity.Payment;
import com.luckxpress.data.entity.LuckUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);
    
    Page<Payment> findByUser(LuckUser user, Pageable pageable);
    
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
    
    Page<Payment> findByType(Payment.PaymentType type, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.type = :type")
    Page<Payment> findByUserIdAndType(@Param("userId") Long userId, @Param("type") Payment.PaymentType type, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<Payment> findPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.type = :type AND p.status = 'COMPLETED' AND p.createdAt >= :startDate")
    BigDecimal getTotalAmountByTypeAndDateAfter(@Param("type") Payment.PaymentType type, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE " +
           "(:userId IS NULL OR p.user.id = :userId) AND " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.createdAt <= :endDate)")
    Page<Payment> findPaymentsWithFilters(
        @Param("userId") Long userId,
        @Param("type") Payment.PaymentType type,
        @Param("status") Payment.PaymentStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}
