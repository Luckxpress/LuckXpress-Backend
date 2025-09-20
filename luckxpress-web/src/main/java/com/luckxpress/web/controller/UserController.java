package com.luckxpress.web.controller;

import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.User;
import com.luckxpress.service.UserService;
import com.luckxpress.web.dto.UserProfileDto;
import com.luckxpress.web.dto.UserRegistrationDto;
import com.luckxpress.web.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

/**
 * User Controller
 * CRITICAL: Provides user management REST endpoints with security
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User registration, profile, and account management")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Register new user
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account with compliance validation"
    )
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid registration data")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<UserResponseDto> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto,
            HttpServletRequest request) {
        
        log.info("User registration request: username={}, email={}, state={}", 
                registrationDto.getUsername(), registrationDto.getEmail(), registrationDto.getStateCode());
        
        String ipAddress = getClientIpAddress(request);
        
        User user = userService.createUser(
            registrationDto.getUsername(),
            registrationDto.getEmail(),
            registrationDto.getPassword(),
            registrationDto.getFirstName(),
            registrationDto.getLastName(),
            registrationDto.getDateOfBirth(),
            registrationDto.getStateCode(),
            ipAddress
        );
        
        UserResponseDto response = UserResponseDto.fromUser(user);
        
        log.info("User registered successfully: userId={}, username={}", user.getId(), user.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieves the authenticated user's profile information"
    )
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<UserResponseDto> getCurrentUserProfile() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        Optional<User> user = userService.findById(currentUserId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserResponseDto response = UserResponseDto.fromUser(user.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update user profile",
        description = "Updates the authenticated user's profile information"
    )
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid profile data")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @Valid @RequestBody UserProfileDto profileDto) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        User updatedUser = userService.updateUserProfile(
            currentUserId,
            profileDto.getFirstName(),
            profileDto.getLastName(),
            profileDto.getPhoneNumber(),
            profileDto.getCity(),
            profileDto.getZipCode()
        );
        
        UserResponseDto response = UserResponseDto.fromUser(updatedUser);
        
        log.info("User profile updated: userId={}", currentUserId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify user email
     */
    @PostMapping("/verify-email")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Verify user email",
        description = "Marks the user's email as verified"
    )
    @ApiResponse(responseCode = "200", description = "Email verified successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<Void> verifyEmail() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        userService.verifyEmail(currentUserId);
        
        log.info("Email verified for user: userId={}", currentUserId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Verify user phone
     */
    @PostMapping("/verify-phone")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Verify user phone",
        description = "Marks the user's phone number as verified"
    )
    @ApiResponse(responseCode = "200", description = "Phone verified successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<Void> verifyPhone() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        userService.verifyPhone(currentUserId);
        
        log.info("Phone verified for user: userId={}", currentUserId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Set self-exclusion period
     */
    @PostMapping("/self-exclusion")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Set self-exclusion period",
        description = "Sets a self-exclusion period for responsible gaming"
    )
    @ApiResponse(responseCode = "200", description = "Self-exclusion set successfully")
    @ApiResponse(responseCode = "400", description = "Invalid exclusion period")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<Void> setSelfExclusion(
            @Parameter(description = "Self-exclusion end time")
            @RequestParam Instant exclusionUntil) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        // Validate exclusion period (minimum 24 hours, maximum 1 year)
        Instant now = Instant.now();
        Instant minExclusion = now.plusSeconds(24 * 60 * 60); // 24 hours
        Instant maxExclusion = now.plusSeconds(365 * 24 * 60 * 60); // 1 year
        
        if (exclusionUntil.isBefore(minExclusion)) {
            return ResponseEntity.badRequest().build();
        }
        
        if (exclusionUntil.isAfter(maxExclusion)) {
            return ResponseEntity.badRequest().build();
        }
        
        userService.setSelfExclusion(currentUserId, exclusionUntil);
        
        log.info("Self-exclusion set: userId={}, until={}", currentUserId, exclusionUntil);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user information by ID (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<UserResponseDto> getUserById(
            @Parameter(description = "User ID")
            @PathVariable String userId) {
        
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserResponseDto response = UserResponseDto.fromUser(user.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Search users (Admin only)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Search users",
        description = "Search users by name, username, or email (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Search results")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @Parameter(description = "Search term")
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<User> users = userService.searchUsers(searchTerm, pageable);
        Page<UserResponseDto> response = users.map(UserResponseDto::fromUser);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Add role to user (Admin only)
     */
    @PostMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add role to user",
        description = "Adds a role to the specified user (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Role added successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> addRole(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Role to add")
            @PathVariable User.UserRole role) {
        
        userService.addRole(userId, role);
        
        log.info("Role added to user: userId={}, role={}, addedBy={}", 
                userId, role, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Remove role from user (Admin only)
     */
    @DeleteMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Remove role from user",
        description = "Removes a role from the specified user (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Role removed successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Role to remove")
            @PathVariable User.UserRole role) {
        
        userService.removeRole(userId, role);
        
        log.info("Role removed from user: userId={}, role={}, removedBy={}", 
                userId, role, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Suspend user (Compliance only)
     */
    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Suspend user",
        description = "Suspends a user account (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "User suspended successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> suspendUser(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Suspension reason")
            @RequestParam String reason) {
        
        userService.suspendUser(userId, reason);
        
        log.warn("User suspended: userId={}, reason={}, suspendedBy={}", 
                userId, reason, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Reactivate user (Compliance only)
     */
    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Reactivate user",
        description = "Reactivates a suspended user account (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "User reactivated successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> reactivateUser(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Reactivation reason")
            @RequestParam String reason) {
        
        userService.reactivateUser(userId, reason);
        
        log.info("User reactivated: userId={}, reason={}, reactivatedBy={}", 
                userId, reason, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Check username availability
     */
    @GetMapping("/check-username/{username}")
    @Operation(
        summary = "Check username availability",
        description = "Checks if a username is available for registration"
    )
    @ApiResponse(responseCode = "200", description = "Username availability checked")
    public ResponseEntity<Boolean> checkUsernameAvailability(
            @Parameter(description = "Username to check")
            @PathVariable String username) {
        
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(available);
    }
    
    /**
     * Check email availability
     */
    @GetMapping("/check-email/{email}")
    @Operation(
        summary = "Check email availability",
        description = "Checks if an email is available for registration"
    )
    @ApiResponse(responseCode = "200", description = "Email availability checked")
    public ResponseEntity<Boolean> checkEmailAvailability(
            @Parameter(description = "Email to check")
            @PathVariable String email) {
        
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(available);
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
