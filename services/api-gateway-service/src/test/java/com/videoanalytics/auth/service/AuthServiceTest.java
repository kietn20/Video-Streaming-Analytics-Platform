/**
 * Auth Service Tests
 * Location: src/test/java/com/videoanalytics/auth/service/AuthServiceTest.java
 */
package com.videoanalytics.auth.service;

import com.videoanalytics.auth.dto.AuthenticationRequest;
import com.videoanalytics.auth.dto.AuthenticationResponse;
import com.videoanalytics.auth.dto.RegistrationRequest;
import com.videoanalytics.auth.model.Role;
import com.videoanalytics.auth.model.Token;
import com.videoanalytics.auth.model.User;
import com.videoanalytics.auth.repository.RoleRepository;
import com.videoanalytics.auth.repository.TokenRepository;
import com.videoanalytics.auth.repository.UserRepository;
import com.videoanalytics.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ReactiveAuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role userRole;
    private Token testToken;

    @BeforeEach
    void setUp() {
        userRole = new Role("ROLE_USER", "Basic user role");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);

        testToken = new Token(
                "refresh-token-123",
                testUser,
                LocalDateTime.now().plusDays(7)
        );
    }

    @Test
    void whenAuthenticate_thenSuccess() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword());

        when(authenticationManager.authenticate(any()))
                .thenReturn(Mono.just(authentication));
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any()))
                .thenReturn("access-token-123");
        when(jwtTokenProvider.getExpirationTime())
                .thenReturn(3600000L);
        when(tokenRepository.save(any()))
                .thenReturn(testToken);

        // When
        Mono<AuthenticationResponse> result = authService.authenticate(request);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    return response.getAccessToken() != null &&
                            response.getRefreshToken() != null &&
                            response.getUsername().equals("testuser");
                })
                .verifyComplete();

        verify(authenticationManager).authenticate(any());
        verify(userRepository).findByUsername("testuser");
        verify(jwtTokenProvider).generateAccessToken(any());
    }

    @Test
    void whenRegister_thenSuccess() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername(anyString()))
                .thenReturn(false);
        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);
        when(roleRepository.findByName(anyString()))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded_password");
        when(userRepository.save(any()))
                .thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any()))
                .thenReturn("access-token-123");
        when(tokenRepository.save(any()))
                .thenReturn(testToken);

        // When
        Mono<AuthenticationResponse> result = authService.register(request);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    return response.getAccessToken() != null &&
                            response.getRefreshToken() != null &&
                            response.getUsername().equals("testuser");
                })
                .verifyComplete();

        verify(userRepository).save(any());
        verify(roleRepository).findByName("ROLE_USER");
    }

    @Test
    void whenRefreshToken_thenSuccess() {
        // Given
        when(tokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(testToken));
        when(jwtTokenProvider.generateAccessToken(any()))
                .thenReturn("new-access-token-123");
        when(tokenRepository.save(any()))
                .thenReturn(testToken);

        // When
        Mono<AuthenticationResponse> result = authService.refreshToken("valid-refresh-token");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    return response.getAccessToken() != null &&
                            response.getRefreshToken() != null;
                })
                .verifyComplete();

        verify(tokenRepository).findByToken("valid-refresh-token");
        verify(jwtTokenProvider).generateAccessToken(testUser);
    }

    @Test
    void whenLogout_thenSuccess() {
        // Given
        when(tokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(testToken));
        when(tokenRepository.save(any()))
                .thenReturn(testToken);

        // When
        Mono<Void> result = authService.logout("valid-refresh-token");

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(tokenRepository).findByToken("valid-refresh-token");
        verify(tokenRepository).save(any());
    }

    @Test
    void whenValidateToken_thenSuccess() {
        // Given
        when(jwtTokenProvider.validateToken(anyString()))
                .thenReturn(true);

        // When
        Mono<Boolean> result = authService.validateToken("valid-token");

        // Then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(jwtTokenProvider).validateToken("valid-token");
    }

    @Test
    void whenRegisterWithExistingUsername_thenFail() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existinguser"))
                .thenReturn(true);

        // When
        Mono<AuthenticationResponse> result = authService.register(request);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void whenRefreshTokenWithInvalidToken_thenFail() {
        // Given
        when(tokenRepository.findByToken(anyString()))
                .thenReturn(Optional.empty());

        // When
        Mono<AuthenticationResponse> result = authService.refreshToken("invalid-token");

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(tokenRepository).findByToken("invalid-token");
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void whenRefreshTokenWithExpiredToken_thenFail() {
        // Given
        Token expiredToken = new Token(
                "expired-token",
                testUser,
                LocalDateTime.now().minusDays(1)
        );

        when(tokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(expiredToken));

        // When
        Mono<AuthenticationResponse> result = authService.refreshToken("expired-token");

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(tokenRepository).findByToken("expired-token");
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }
}