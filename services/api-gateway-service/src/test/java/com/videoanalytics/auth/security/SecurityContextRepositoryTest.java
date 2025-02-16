/**
 * Security Context Repository Tests
 * Location: src/test/java/com/videoanalytics/auth/security/SecurityContextRepositoryTest.java
 */
package com.videoanalytics.auth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextRepositoryTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private SecurityContextRepository securityContextRepository;

    @Test
    void whenValidToken_thenLoadsContext() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "Bearer valid.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user", null, Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authenticationManager.authenticate(any()))
                .thenReturn(Mono.just(authentication));

        // When
        Mono<SecurityContext> context = securityContextRepository.load(exchange);

        // Then
        StepVerifier.create(context)
                .expectNextMatches(ctx ->
                        ctx.getAuthentication() != null &&
                                ctx.getAuthentication().getName().equals("user")
                )
                .verifyComplete();
    }

    @Test
    void whenNoAuthHeader_thenEmptyContext() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<SecurityContext> context = securityContextRepository.load(exchange);

        // Then
        StepVerifier.create(context)
                .verifyComplete();
    }

    @Test
    void whenInvalidAuthHeaderFormat_thenEmptyContext() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "InvalidFormat token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<SecurityContext> context = securityContextRepository.load(exchange);

        // Then
        StepVerifier.create(context)
                .verifyComplete();
    }

    @Test
    void whenSavingContext_thenReturnsEmpty() {
        // Given
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").build()
        );
        SecurityContext securityContext = new SecurityContextImpl();

        // When
        Mono<Void> result = securityContextRepository.save(exchange, securityContext);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}