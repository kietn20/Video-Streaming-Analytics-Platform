/**
 * Custom User Details Service
 * Location: src/main/java/com/videoanalytics/security/jwt/CustomUserDetailsService.java
 *
 * This service loads user details from the database for Spring Security authentication.
 */
package com.videoanalytics.security.jwt;

import com.videoanalytics.auth.model.User;
import com.videoanalytics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to load user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Convert User entity to UserPrincipal for Spring Security
        return UserPrincipal.create(user);
    }

    /**
     * Loads user by ID (useful for token-based authentication).
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserPrincipal.create(user);
    }
}