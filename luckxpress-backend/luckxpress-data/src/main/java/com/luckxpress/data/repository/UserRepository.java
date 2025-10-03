package com.luckxpress.data.repository;

import com.luckxpress.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 * Provides database access operations for users
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * @param username the username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * @param email the email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     * @param username the username
     * @param email the email
     * @return Optional containing user if found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if username exists
     * @param username the username
     * @return true if exists
     */
    Boolean existsByUsername(String username);

    /**
     * Check if email exists
     * @param email the email
     * @return true if exists
     */
    Boolean existsByEmail(String email);

    /**
     * Find all users with pagination
     * @param pageable pagination parameters
     * @return page of users
     */
    Page<User> findAllByIsActiveTrue(Pageable pageable);

    /**
     * Find users by role name
     * @param roleName the role name
     * @param pageable pagination parameters
     * @return page of users with the specified role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    /**
     * Find users whose account is locked
     * @return list of locked users
     */
    List<User> findByAccountLockedTrue();

    /**
     * Find users whose credentials have expired
     * @return list of users with expired credentials
     */
    List<User> findByCredentialsExpiredTrue();

    /**
     * Find users by email verification status
     * @param verified verification status
     * @return list of users
     */
    List<User> findByEmailVerified(Boolean verified);

    /**
     * Update last login date
     * @param userId user ID
     * @param loginDate login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginDate = :loginDate WHERE u.id = :userId")
    void updateLastLoginDate(@Param("userId") Long userId, @Param("loginDate") LocalDateTime loginDate);

    /**
     * Update failed login attempts
     * @param userId user ID
     * @param attempts number of failed attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("attempts") Integer attempts);

    /**
     * Lock user account
     * @param userId user ID
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true WHERE u.id = :userId")
    void lockUserAccount(@Param("userId") Long userId);

    /**
     * Unlock user account
     * @param userId user ID
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = false, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockUserAccount(@Param("userId") Long userId);

    /**
     * Search users by keyword (username, email, first name, last name)
     * @param keyword search keyword
     * @param pageable pagination parameters
     * @return page of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND u.isActive = true")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find inactive users who haven't logged in for specified days
     * @param days number of days
     * @return list of inactive users
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginDate < :date OR u.lastLoginDate IS NULL")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date);

    /**
     * Count users by role
     * @param roleName the role name
     * @return count of users with the role
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true")
    Long countByRole(@Param("roleName") String roleName);
}
