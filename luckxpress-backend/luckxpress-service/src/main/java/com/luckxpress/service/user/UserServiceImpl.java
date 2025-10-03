package com.luckxpress.service.user;

import com.luckxpress.core.context.SecurityContextHelper;
import com.luckxpress.core.security.JwtService;
import com.luckxpress.data.entity.Role;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.RoleRepository;
import com.luckxpress.data.repository.UserRepository;
import com.luckxpress.service.dto.UserDTO;
import com.luckxpress.service.security.CustomUserDetailsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for user management operations
 * Provides business logic for user authentication, registration, and profile management
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityContextHelper securityContextHelper;
    private final ModelMapper modelMapper;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            SecurityContextHelper securityContextHelper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityContextHelper = securityContextHelper;
        this.modelMapper = createModelMapper();
    }

    private ModelMapper createModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setSkipNullEnabled(true);
        return mapper;
    }

    @Override
    public UserDTO registerUser(UserDTO.RegisterRequest registerRequest) {
        logger.info("Processing registration for username: {}", registerRequest.getUsername());
        
        validateNewUser(registerRequest.getUsername(), registerRequest.getEmail());
        
        User newUser = buildNewUser(registerRequest);
        assignDefaultRole(newUser);
        
        User savedUser = userRepository.save(newUser);
        logger.info("User registered successfully with ID: {}", savedUser.getId());
        
        return mapToUserDTO(savedUser);
    }

    @Override
    public UserDTO.AuthResponse authenticateUser(UserDTO.LoginRequest loginRequest) {
        logger.debug("Processing authentication request");
        
        try {
            Authentication auth = performAuthentication(loginRequest);
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            
            User user = getUserByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            updateSuccessfulLogin(user);
            
            return buildAuthResponse(accessToken, refreshToken, user);
            
        } catch (BadCredentialsException e) {
            handleFailedAuthentication(loginRequest.getUsernameOrEmail());
            throw new BadCredentialsException("Invalid credentials provided");
        }
    }

    @Override
    public UserDTO.AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        if (!jwtService.validateToken(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        
        String newAccessToken = jwtService.generateToken(userDetails);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return buildAuthResponse(newAccessToken, refreshToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id).map(this::mapToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::mapToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::mapToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> findAll(Pageable pageable) {
        return userRepository.findAllByIsActiveTrue(pageable).map(this::mapToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable).map(this::mapToUserDTO);
    }

    @Override
    public UserDTO updateProfile(Long userId, UserDTO.ProfileUpdateRequest updateRequest) {
        User user = getUserById(userId);
        
        updateUserProfile(user, updateRequest);
        User updatedUser = userRepository.save(user);
        
        logger.info("Profile updated for user ID: {}", userId);
        return mapToUserDTO(updatedUser);
    }

    @Override
    public void changePassword(Long userId, UserDTO.PasswordChangeRequest changeRequest) {
        User user = getUserById(userId);
        
        validatePasswordChange(user, changeRequest);
        updateUserPassword(user, changeRequest.getNewPassword());
        
        userRepository.save(user);
        logger.info("Password changed for user ID: {}", userId);
    }

    @Override
    public void initiatePasswordReset(UserDTO.PasswordResetRequest resetRequest) {
        Optional<User> userOpt = userRepository.findByEmail(resetRequest.getEmail());
        if (userOpt.isPresent()) {
            // TODO: Generate reset token and send email
            logger.info("Password reset initiated for email: {}", resetRequest.getEmail());
        }
    }

    @Override
    public void completePasswordReset(String token, String newPassword) {
        // TODO: Validate token and update password
        logger.info("Password reset completed");
    }

    @Override
    public void verifyEmail(String token) {
        // TODO: Implement email verification logic
        logger.info("Email verification processed");
    }

    @Override
    public void lockAccount(Long userId) {
        userRepository.lockUserAccount(userId);
        logger.warn("Account locked for user ID: {}", userId);
    }

    @Override
    public void unlockAccount(Long userId) {
        userRepository.unlockUserAccount(userId);
        logger.info("Account unlocked for user ID: {}", userId);
    }

    @Override
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        logger.info("User deactivated with ID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getCurrentUser() {
        return securityContextHelper.getCurrentUsername()
                .flatMap(userRepository::findByUsername)
                .map(this::mapToUserDTO);
    }

    @Override
    public void updateLastLogin(Long userId) {
        userRepository.updateLastLoginDate(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> findByRole(String roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, pageable).map(this::mapToUserDTO);
    }

    @Override
    public void assignRole(Long userId, String roleName) {
        User user = getUserById(userId);
        Role role = getRole(roleName);
        
        user.addRole(role);
        userRepository.save(user);
        logger.info("Role {} assigned to user ID: {}", roleName, userId);
    }

    @Override
    public void removeRole(Long userId, String roleName) {
        User user = getUserById(userId);
        Role role = getRole(roleName);
        
        user.removeRole(role);
        userRepository.save(user);
        logger.info("Role {} removed from user ID: {}", roleName, userId);
    }

    // Helper methods
    private void validateNewUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
    }

    private User buildNewUser(UserDTO.RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        return user;
    }

    private void assignDefaultRole(User user) {
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not configured"));
        user.addRole(defaultRole);
    }

    private Authentication performAuthentication(UserDTO.LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );
    }

    private User getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void updateSuccessfulLogin(User user) {
        userRepository.updateLastLoginDate(user.getId(), LocalDateTime.now());
        userRepository.updateFailedLoginAttempts(user.getId(), 0);
    }

    private void handleFailedAuthentication(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .ifPresent(this::incrementFailedAttempts);
    }

    private void incrementFailedAttempts(User user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        userRepository.updateFailedLoginAttempts(user.getId(), newAttempts);
        
        if (newAttempts >= MAX_LOGIN_ATTEMPTS) {
            userRepository.lockUserAccount(user.getId());
            logger.warn("Account locked after {} failed attempts: {}", MAX_LOGIN_ATTEMPTS, user.getUsername());
        }
    }

    private UserDTO.AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return UserDTO.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(mapToUserDTO(user))
                .build();
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    private Role getRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
    }

    private void updateUserProfile(User user, UserDTO.ProfileUpdateRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
    }

    private void validatePasswordChange(User user, UserDTO.PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
    }

    private void updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChangeDate(LocalDateTime.now());
    }

    private UserDTO mapToUserDTO(User user) {
        UserDTO dto = modelMapper.map(user, UserDTO.class);
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        dto.setRoles(roleNames);
        return dto;
    }
}
