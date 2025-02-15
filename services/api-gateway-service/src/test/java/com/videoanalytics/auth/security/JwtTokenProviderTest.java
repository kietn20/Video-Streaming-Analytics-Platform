/**
 * JWT Token Provider Tests
 * Location: src/test/java/com/videoanalytics/auth/security/JwtTokenProviderTest.java
 */
package com.videoanalytics.auth.security;

import com.videoanalytics.auth.model.Role;
import com.videoanalytics.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "testsecretkeytestsecretkeytestsecretkey");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 3600000L);

        Role userRole = new Role("ROLE_USER", "Basic user role");
        testUser = new User();
        testUser.setUsername("testuser");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Test
    void whenGenerateToken_thenSuccess() {
        // When
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token))
                .isEqualTo(testUser.getUsername());
    }

    @Test
    void whenValidateToken_withValidToken_thenSuccess() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void whenValidateToken_withInvalidToken_thenFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("invalid.token.here");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void whenGetUsernameFromToken_thenSuccess() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    @Test
    void whenGetExpirationTime_thenSuccess() {
        // When
        Long expirationTime = jwtTokenProvider.getExpirationTime();

        // Then
        assertThat(expirationTime).isEqualTo(3600000L);
    }
}