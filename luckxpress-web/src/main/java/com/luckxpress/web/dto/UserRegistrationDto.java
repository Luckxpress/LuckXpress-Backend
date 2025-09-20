package com.luckxpress.web.dto;

import com.luckxpress.common.validation.ValidState;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * User Registration DTO
 * CRITICAL: User registration request with validation
 */
@Data
public class UserRegistrationDto {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @NotBlank(message = "State code is required")
    @ValidState(message = "Invalid state or state not supported for sweepstakes")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    private String stateCode;
    
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;
    
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Invalid ZIP code format")
    private String zipCode;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @AssertTrue(message = "You must be at least 21 years old to register")
    public boolean isOfLegalAge() {
        if (dateOfBirth == null) return false;
        return LocalDate.now().getYear() - dateOfBirth.getYear() >= 21;
    }
}
