package com.luckxpress.service.user;

import com.luckxpress.service.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service interface for user-related operations
 */
public interface UserService {

    /**
     * Register a new user
     * @param registerRequest registration details
     * @return created user DTO
     */
    UserDTO registerUser(UserDTO.RegisterRequest registerRequest);

    /**
     * Authenticate user and generate tokens
     * @param loginRequest login credentials
     * @return authentication response with tokens
     */
    UserDTO.AuthResponse authenticateUser(UserDTO.LoginRequest loginRequest);

    /**
     * Refresh authentication token
     * @param refreshToken the refresh token
     * @return new authentication response
     */
    UserDTO.AuthResponse refreshToken(String refreshToken);

    /**
     * Find user by ID
     * @param id user ID
     * @return user DTO if found
     */
    Optional<UserDTO> findById(Long id);

    /**
     * Find user by username
     * @param username the username
     * @return user DTO if found
     */
    Optional<UserDTO> findByUsername(String username);

    /**
     * Find user by email
     * @param email the email
     * @return user DTO if found
     */
    Optional<UserDTO> findByEmail(String email);

    /**
     * Get all users with pagination
     * @param pageable pagination parameters
     * @return page of users
     */
    Page<UserDTO> findAll(Pageable pageable);

    /**
     * Search users by keyword
     * @param keyword search term
     * @param pageable pagination parameters
     * @return page of matching users
     */
    Page<UserDTO> searchUsers(String keyword, Pageable pageable);

    /**
     * Update user profile
     * @param userId user ID
     * @param updateRequest profile update details
     * @return updated user DTO
     */
    UserDTO updateProfile(Long userId, UserDTO.ProfileUpdateRequest updateRequest);

    /**
     * Change user password
     * @param userId user ID
     * @param changeRequest password change details
     */
    void changePassword(Long userId, UserDTO.PasswordChangeRequest changeRequest);

    /**
     * Initiate password reset
     * @param resetRequest password reset request
     */
    void initiatePasswordReset(UserDTO.PasswordResetRequest resetRequest);

    /**
     * Complete password reset
     * @param token reset token
     * @param newPassword new password
     */
    void completePasswordReset(String token, String newPassword);

    /**
     * Verify user email
     * @param token verification token
     */
    void verifyEmail(String token);

    /**
     * Lock user account
     * @param userId user ID
     */
    void lockAccount(Long userId);

    /**
     * Unlock user account
     * @param userId user ID
     */
    void unlockAccount(Long userId);

    /**
     * Delete user
     * @param userId user ID
     */
    void deleteUser(Long userId);

    /**
     * Check if username exists
     * @param username the username
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * @param email the email
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Get current authenticated user
     * @return current user DTO
     */
    Optional<UserDTO> getCurrentUser();

    /**
     * Update last login time
     * @param userId user ID
     */
    void updateLastLogin(Long userId);

    /**
     * Get users by role
     * @param roleName role name
     * @param pageable pagination parameters
     * @return page of users with the role
     */
    Page<UserDTO> findByRole(String roleName, Pageable pageable);

    /**
     * Assign role to user
     * @param userId user ID
     * @param roleName role name
     */
    void assignRole(Long userId, String roleName);

    /**
     * Remove role from user
     * @param userId user ID
     * @param roleName role name
     */
    void removeRole(Long userId, String roleName);
}
