/**
 * Authentication Manager Tests
 * Location: src/test/java/com/videoanalytics/auth/security/AuthenticationManagerTest.java
 */
package com.videoanalytics.auth.security;

import com.videoanalytics.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationManagerTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthenticationManager authenticationManager;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.token";

    @BeforeEach
    void setUp() {
        when(authService.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(true));
        when(authService.validateToken(INVALID_TOKEN))
                .thenReturn(Mono.just(false));

        when(tokenProvider.getUsernameFromToken(VALID_TOKEN))
                .thenReturn("testuser");
        when(tokenProvider.getRolesFromToken(VALID_TOKEN))
                .thenReturn(Arrays.asList("ROLE_USER"));
    }

    @Test
    void whenValidToken_thenAuthenticates() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken(null, VALID_TOKEN);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(auth);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(authentication ->
                        authentication.getName().equals("testuser") &&
                                authentication.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))
                )
                .verifyComplete();
    }

    @Test
    void whenInvalidToken_thenAuthenticationFails() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken(null, INVALID_TOKEN);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(auth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void whenNullToken_thenAuthenticationFails() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken(null, null);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(auth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}