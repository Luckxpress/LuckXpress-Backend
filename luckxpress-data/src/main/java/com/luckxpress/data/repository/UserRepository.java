package com.luckxpress.data.repository;

import com.luckxpress.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * User Repository
 * CRITICAL: Provides data access for user management and compliance queries
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by username or email
     */
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by state code
     */
    List<User> findByStateCode(String stateCode);
    
    /**
     * Find users by status
     */
    List<User> findByStatus(User.UserStatus status);
    
    /**
     * Find users by KYC status
     */
    List<User> findByKycStatus(User.KycStatus kycStatus);
    
    /**
     * Find KYC verified users
     */
    @Query("SELECT u FROM User u WHERE u.kycStatus = 'APPROVED'")
    List<User> findKycVerifiedUsers();
    
    /**
     * Find users requiring KYC verification
     */
    @Query("SELECT u FROM User u WHERE u.kycStatus IN ('PENDING', 'IN_PROGRESS', 'EXPIRED')")
    List<User> findUsersRequiringKyc();
    
    /**
     * Find users by role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") User.UserRole role);
    
    /**
     * Find admin users
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r IN ('ADMIN', 'SUPER_ADMIN')")
    List<User> findAdminUsers();
    
    /**
     * Find compliance officers
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = 'COMPLIANCE_OFFICER'")
    List<User> findComplianceOfficers();
    
    /**
     * Find users with locked accounts
     */
    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > :now")
    List<User> findLockedUsers(@Param("now") Instant now);
    
    /**
     * Find self-excluded users
     */
    @Query("SELECT u FROM User u WHERE u.selfExclusionUntil IS NOT NULL AND u.selfExclusionUntil > :now")
    List<User> findSelfExcludedUsers(@Param("now") Instant now);
    
    /**
     * Find users created within date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") Instant startDate, 
                                     @Param("endDate") Instant endDate);
    
    /**
     * Find users with failed login attempts
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    List<User> findUsersWithFailedLogins(@Param("threshold") Integer threshold);
    
    /**
     * Find inactive users (no login within specified days)
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate")
    Page<User> findInactiveUsers(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);
    
    /**
     * Find users by phone number
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * Find users with unverified email
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = false")
    List<User> findUsersWithUnverifiedEmail();
    
    /**
     * Find users with unverified phone
     */
    @Query("SELECT u FROM User u WHERE u.phoneVerified = false AND u.phoneNumber IS NOT NULL")
    List<User> findUsersWithUnverifiedPhone();
    
    /**
     * Search users by name or username
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Count users by status
     */
    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> countUsersByStatus();
    
    /**
     * Count users by KYC status
     */
    @Query("SELECT u.kycStatus, COUNT(u) FROM User u GROUP BY u.kycStatus")
    List<Object[]> countUsersByKycStatus();
    
    /**
     * Count users by state
     */
    @Query("SELECT u.stateCode, COUNT(u) FROM User u GROUP BY u.stateCode ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByState();
    
    /**
     * Find users registered today
     */
    @Query("SELECT u FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    List<User> findUsersRegisteredToday();
    
    /**
     * Find VIP users
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = 'VIP'")
    List<User> findVipUsers();
    
    /**
     * Update last login information
     */
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ipAddress, " +
           "u.failedLoginAttempts = 0, u.accountLockedUntil = null WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId, 
                        @Param("loginTime") Instant loginTime, 
                        @Param("ipAddress") String ipAddress);
    
    /**
     * Increment failed login attempts
     */
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") String userId);
    
    /**
     * Lock user account
     */
    @Query("UPDATE User u SET u.accountLockedUntil = :lockUntil WHERE u.id = :userId")
    void lockUserAccount(@Param("userId") String userId, @Param("lockUntil") Instant lockUntil);
}
