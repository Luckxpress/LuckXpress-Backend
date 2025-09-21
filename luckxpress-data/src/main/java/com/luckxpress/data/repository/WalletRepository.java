package com.luckxpress.data.repository;

import com.luckxpress.data.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Wallet repository with optimistic locking
 * CRITICAL: Use locks for concurrent balance updates
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    
    /**
     * Find wallet by user ID
     */
    Optional<Wallet> findByUserId(String userId);
    
    /**
     * Find wallet by user ID with pessimistic lock
     * CRITICAL: Use for balance updates to prevent race conditions
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") String userId);
    
    /**
     * Check if wallet exists for user
     */
    boolean existsByUserId(String userId);
    
    /**
     * Find all wallets with Gold balance above threshold
     */
    @Query("SELECT w FROM Wallet w WHERE w.goldBalance >= :threshold")
    List<Wallet> findWalletsWithGoldAbove(@Param("threshold") BigDecimal threshold);
    
    /**
     * Find all wallets with Sweeps balance above threshold
     */
    @Query("SELECT w FROM Wallet w WHERE w.sweepsBalance >= :threshold")
    List<Wallet> findWalletsWithSweepsAbove(@Param("threshold") BigDecimal threshold);
    
    /**
     * Get total Gold balance across all wallets
     */
    @Query("SELECT COALESCE(SUM(w.goldBalance), 0) FROM Wallet w")
    BigDecimal getTotalGoldBalance();
    
    /**
     * Get total Sweeps balance across all wallets
     */
    @Query("SELECT COALESCE(SUM(w.sweepsBalance), 0) FROM Wallet w")
    BigDecimal getTotalSweepsBalance();
    
    /**
     * Find wallets with locked funds
     */
    @Query("SELECT w FROM Wallet w WHERE w.goldLocked > 0 OR w.sweepsLocked > 0")
    List<Wallet> findWalletsWithLockedFunds();
    
    /**
     * Get high-value wallets for compliance monitoring
     */
    @Query("SELECT w FROM Wallet w WHERE (w.goldBalance + w.sweepsBalance) >= :threshold ORDER BY (w.goldBalance + w.sweepsBalance) DESC")
    List<Wallet> findHighValueWallets(@Param("threshold") BigDecimal threshold);
}
