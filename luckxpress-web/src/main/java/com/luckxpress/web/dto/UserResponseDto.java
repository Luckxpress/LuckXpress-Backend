package com.luckxpress.web.dto;

import com.luckxpress.data.entity.User;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * User Response DTO
 * CRITICAL: Sanitized user data for API responses
 */
@Data
public class UserResponseDto {
    
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String stateCode;
    private String city;
    private String zipCode;
    private User.UserStatus status;
    private User.KycStatus kycStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Instant kycVerifiedAt;
    private Instant lastLoginAt;
    private Instant selfExclusionUntil;
    private Set<User.UserRole> roles;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static UserResponseDto fromUser(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setStateCode(user.getStateCode());
        dto.setCity(user.getCity());
        dto.setZipCode(user.getZipCode());
        dto.setStatus(user.getStatus());
        dto.setKycStatus(user.getKycStatus());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setPhoneVerified(user.getPhoneVerified());
        dto.setKycVerifiedAt(user.getKycVerifiedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setSelfExclusionUntil(user.getSelfExclusionUntil());
        dto.setRoles(user.getRoles());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
