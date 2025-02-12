/**
 * JWT Authentication Filter Tests
 * Location: src/test/java/com/videoanalytics/gateway/filter/JwtAuthenticationFilterTest.java
 */
package com.videoanalytics.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.web-application-type=reactive")
class JwtAuthenticationFilterTest {

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final String INVALID_TOKEN = "Bearer invalid.jwt.token";
    private static final String MALFORMED_TOKEN = "invalid-format-token";

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtAuthenticationFilter();
    }

    @Test
    void whenValidToken_thenAcceptsRequest() {
        // Create mock request with valid token
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("Authorization", VALID_TOKEN)
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());

        // Execute filter and verify
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    void whenInvalidToken_thenRejectsRequest() {
        // Create mock request with invalid token
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("Authorization", INVALID_TOKEN)
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());

        // Execute filter and verify
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        StepVerifier.create(result)
                .expectError(Exception.class)
                .verify();
    }

    @Test
    void whenMalformedToken_thenRejectsRequest() {
        // Create mock request with malformed token
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("Authorization", MALFORMED_TOKEN)
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());

        // Execute filter and verify
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        StepVerifier.create(result)
                .expectError(Exception.class)
                .verify();
    }

    @Test
    void whenNoToken_thenRejectsRequest() {
        // Create mock request without token
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = jwtFilter.apply(new JwtAuthenticationFilter.Config());

        // Execute filter and verify
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        StepVerifier.create(result)
                .expectError(Exception.class)
                .verify();
    }
}