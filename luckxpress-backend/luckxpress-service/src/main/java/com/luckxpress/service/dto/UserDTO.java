package com.luckxpress.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for User entity
 * Used for transferring user data between layers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {

    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    private String profilePictureUrl;

    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean accountLocked;
    private Boolean accountExpired;
    private Boolean credentialsExpired;
    private Boolean isActive;

    private LocalDateTime lastLoginDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

    private Set<String> roles;

    /**
     * DTO for user registration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        private String password;

        private String firstName;
        private String lastName;
        private String phoneNumber;
    }

    /**
     * DTO for user login
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username or email is required")
        private String usernameOrEmail;

        @NotBlank(message = "Password is required")
        private String password;
    }

    /**
     * DTO for authentication response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserDTO user;
    }

    /**
     * DTO for password change
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        private String newPassword;

        @NotBlank(message = "Password confirmation is required")
        private String confirmPassword;
    }

    /**
     * DTO for password reset
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
    }

    /**
     * DTO for user profile update
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        private String phoneNumber;

        private String profilePictureUrl;
    }
}
