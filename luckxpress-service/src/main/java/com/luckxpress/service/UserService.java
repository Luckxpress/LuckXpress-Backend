package com.luckxpress.service;

import com.luckxpress.common.constants.StateRestriction;
import com.luckxpress.common.exception.ComplianceException;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.ComplianceAudit;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * User Service
 * CRITICAL: Manages user lifecycle with compliance validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ComplianceService complianceService;
    private final AccountService accountService;
    private final AuditService auditService;
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Find user by username or email
     */
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }
    
    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    /**
     * Create new user with compliance validation
     */
    @Transactional
    public User createUser(String username, String email, String password, 
                          String firstName, String lastName, LocalDate dateOfBirth,
                          String stateCode, String ipAddress) {
        
        log.info("Creating new user: username={}, email={}, state={}", username, email, stateCode);
        
        // Validate compliance requirements
        validateUserCreation(username, email, stateCode, dateOfBirth, ipAddress);
        
        // Create user entity
        User user = new User();
        user.setId(IdGenerator.generateId("USR"));
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDateOfBirth(dateOfBirth);
        user.setStateCode(stateCode.toUpperCase());
        user.setStatus(User.UserStatus.PENDING_VERIFICATION);
        user.setKycStatus(User.KycStatus.PENDING);
        user.setRoles(Set.of(User.UserRole.USER));
        user.setTermsAcceptedAt(Instant.now());
        user.setPrivacyAcceptedAt(Instant.now());
        
        // Save user
        user = userRepository.save(user);
        
        // Create user accounts for both currencies
        accountService.createUserAccounts(user);
        
        // Log user creation
        auditService.logUserCreated(user, ipAddress);
        
        log.info("User created successfully: userId={}, username={}", user.getId(), user.getUsername());
        
        return user;
    }
    
    /**
     * Update user profile
     */
    @Transactional
    public User updateUserProfile(String userId, String firstName, String lastName, 
                                 String phoneNumber, String city, String zipCode) {
        
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Validate user can be updated
        if (!user.isActive()) {
            throw new IllegalStateException("Cannot update inactive user profile");
        }
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setCity(city);
        user.setZipCode(zipCode);
        
        user = userRepository.save(user);
        
        auditService.logUserProfileUpdated(user);
        
        return user;
    }
    
    /**
     * Verify user email
     */
    @Transactional
    public void verifyEmail(String userId) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        
        // Activate user if this was the only verification needed
        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        
        userRepository.save(user);
        
        auditService.logEmailVerified(user);
        
        log.info("Email verified for user: userId={}", userId);
    }
    
    /**
     * Verify user phone number
     */
    @Transactional
    public void verifyPhone(String userId) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(Instant.now());
        
        userRepository.save(user);
        
        auditService.logPhoneVerified(user);
        
        log.info("Phone verified for user: userId={}", userId);
    }
    
    /**
     * Record successful login
     */
    @Transactional
    public void recordSuccessfulLogin(String userId, String ipAddress) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);
        
        auditService.logSuccessfulLogin(user, ipAddress);
    }
    
    /**
     * Record failed login attempt
     */
    @Transactional
    public void recordFailedLogin(String userId) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.recordFailedLogin();
        userRepository.save(user);
        
        auditService.logFailedLogin(user);
        
        if (user.isAccountLocked()) {
            log.warn("User account locked due to failed login attempts: userId={}", userId);
        }
    }
    
    /**
     * Set self-exclusion period
     */
    @Transactional
    public void setSelfExclusion(String userId, Instant exclusionUntil) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setSelfExclusion(exclusionUntil);
        userRepository.save(user);
        
        auditService.logSelfExclusionSet(user, exclusionUntil);
        
        log.info("Self-exclusion set for user: userId={}, until={}", userId, exclusionUntil);
    }
    
    /**
     * Add role to user
     */
    @Transactional
    public void addRole(String userId, User.UserRole role) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Validate current user has permission to assign roles
        if (!SecurityContext.isAdmin()) {
            throw new IllegalStateException("Only admins can assign roles");
        }
        
        user.getRoles().add(role);
        userRepository.save(user);
        
        auditService.logRoleAdded(user, role);
        
        log.info("Role added to user: userId={}, role={}", userId, role);
    }
    
    /**
     * Remove role from user
     */
    @Transactional
    public void removeRole(String userId, User.UserRole role) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Validate current user has permission to remove roles
        if (!SecurityContext.isAdmin()) {
            throw new IllegalStateException("Only admins can remove roles");
        }
        
        user.getRoles().remove(role);
        userRepository.save(user);
        
        auditService.logRoleRemoved(user, role);
        
        log.info("Role removed from user: userId={}, role={}", userId, role);
    }
    
    /**
     * Suspend user account
     */
    @Transactional
    public void suspendUser(String userId, String reason) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Validate current user has permission to suspend
        if (!SecurityContext.isComplianceOfficer()) {
            throw new IllegalStateException("Only compliance officers can suspend users");
        }
        
        user.setStatus(User.UserStatus.SUSPENDED);
        user.setNotes(reason);
        
        userRepository.save(user);
        
        auditService.logUserSuspended(user, reason);
        
        log.warn("User suspended: userId={}, reason={}", userId, reason);
    }
    
    /**
     * Reactivate suspended user
     */
    @Transactional
    public void reactivateUser(String userId, String reason) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Validate current user has permission to reactivate
        if (!SecurityContext.isComplianceOfficer()) {
            throw new IllegalStateException("Only compliance officers can reactivate users");
        }
        
        user.setStatus(User.UserStatus.ACTIVE);
        user.setNotes(reason);
        
        userRepository.save(user);
        
        auditService.logUserReactivated(user, reason);
        
        log.info("User reactivated: userId={}, reason={}", userId, reason);
    }
    
    /**
     * Search users
     */
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable);
    }
    
    /**
     * Find users by state
     */
    public List<User> findUsersByState(String stateCode) {
        return userRepository.findByStateCode(stateCode);
    }
    
    /**
     * Find users requiring KYC
     */
    public List<User> findUsersRequiringKyc() {
        return userRepository.findUsersRequiringKyc();
    }
    
    /**
     * Find inactive users
     */
    public Page<User> findInactiveUsers(int daysSinceLastLogin, Pageable pageable) {
        Instant cutoffDate = Instant.now().minusSeconds(daysSinceLastLogin * 24 * 60 * 60);
        return userRepository.findInactiveUsers(cutoffDate, pageable);
    }
    
    /**
     * Validate user creation compliance
     */
    private void validateUserCreation(String username, String email, String stateCode, 
                                    LocalDate dateOfBirth, String ipAddress) {
        
        // Check username availability
        if (!isUsernameAvailable(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        // Check email availability
        if (!isEmailAvailable(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        // Validate age (must be 21+)
        if (dateOfBirth != null) {
            int age = LocalDate.now().getYear() - dateOfBirth.getYear();
            if (age < 21) {
                auditService.createComplianceAudit(
                    ComplianceAudit.EventType.UNDERAGE_USER_DETECTED,
                    ComplianceAudit.Severity.HIGH,
                    "Underage user registration attempt: age=" + age,
                    null
                );
                throw new ComplianceException(
                    ComplianceException.ComplianceType.AGE_VERIFICATION_FAILED,
                    "Must be 21 or older to register",
                    null
                );
            }
        }
        
        // Validate state restrictions
        if (StateRestriction.isStateRestricted(stateCode)) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.BLOCKED_STATE_ACCESS_ATTEMPT,
                ComplianceAudit.Severity.HIGH,
                "Registration attempt from restricted state: " + stateCode,
                null
            );
            throw ComplianceException.stateRestriction(stateCode, null);
        }
        
        log.info("User creation validation passed: username={}, state={}", username, stateCode);
    }
}
