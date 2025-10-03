package com.luckxpress.service.security;

import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom implementation of UserDetailsService for Spring Security
 * Loads user details from the database for authentication
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

        return createUserDetails(user);
    }

    /**
     * Load user by ID
     * @param userId the user ID
     * @return UserDetails
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + userId));

        return createUserDetails(user);
    }

    /**
     * Creates UserDetails from User entity
     * @param user the user entity
     * @return UserDetails implementation
     */
    private UserDetails createUserDetails(User user) {
        Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return new CustomUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.isEnabled(),
                !Boolean.TRUE.equals(user.getAccountExpired()),
                user.isCredentialsNonExpired(),
                !Boolean.TRUE.equals(user.getAccountLocked())
        );
    }

    /**
     * Custom UserDetails implementation
     */
    public static class CustomUserPrincipal implements UserDetails {
        private final Long id;
        private final String username;
        private final String email;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean enabled;
        private final boolean accountNonExpired;
        private final boolean credentialsNonExpired;
        private final boolean accountNonLocked;

        public CustomUserPrincipal(Long id, String username, String email, String password,
                                  Collection<? extends GrantedAuthority> authorities,
                                  boolean enabled, boolean accountNonExpired,
                                  boolean credentialsNonExpired, boolean accountNonLocked) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.password = password;
            this.authorities = authorities;
            this.enabled = enabled;
            this.accountNonExpired = accountNonExpired;
            this.credentialsNonExpired = credentialsNonExpired;
            this.accountNonLocked = accountNonLocked;
        }

        public Long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return accountNonExpired;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return credentialsNonExpired;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}
