package com.luckxpress.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity representing system users
 * Contains user authentication and profile information
 */
@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "email")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(min = 6, max = 255)
    @Column(name = "password", nullable = false)
    private String password;

    @Size(max = 50)
    @Column(name = "first_name")
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name")
    private String lastName;

    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Column(name = "account_expired")
    private Boolean accountExpired = false;

    @Column(name = "credentials_expired")
    private Boolean credentialsExpired = false;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "last_password_change_date")
    private LocalDateTime lastPasswordChangeDate;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Size(max = 500)
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Adds a role to the user
     * @param role the role to add
     */
    public void addRole(Role role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Removes a role from the user
     * @param role the role to remove
     */
    public void removeRole(Role role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Checks if the user has a specific role
     * @param roleName the name of the role to check
     * @return true if the user has the role
     */
    public boolean hasRole(String roleName) {
        return roles != null && roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Gets the full name of the user
     * @return full name combining first and last name
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        return fullName.toString();
    }

    /**
     * Checks if the account is enabled
     * @return true if the account is not locked, expired, and is active
     */
    public boolean isEnabled() {
        return getIsActive() && 
               !Boolean.TRUE.equals(accountLocked) && 
               !Boolean.TRUE.equals(accountExpired);
    }

    /**
     * Checks if the credentials are non-expired
     * @return true if credentials are not expired
     */
    public boolean isCredentialsNonExpired() {
        return !Boolean.TRUE.equals(credentialsExpired);
    }
}
