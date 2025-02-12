/**
 * Rate Limiting Filter Tests
 * Location: src/test/java/com/videoanalytics/gateway/filter/RateLimitingFilterTest.java
 */
package com.videoanalytics.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class RateLimitingFilterTest {

    @Container
    public static final GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @MockBean
    private ReactiveRedisTemplate<String, Long> redisTemplate;

    @MockBean
    private ReactiveValueOperations<String, Long> valueOperations;

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setMaxRequests(10);
        config.setPerSeconds(60);

        rateLimitingFilter = new RateLimitingFilter(redisTemplate);
    }

    @Test
    void whenUnderRateLimit_thenAllowsRequest() {
        // Mock Redis to return count under limit
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(5L));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Execute filter
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        // Verify request is allowed
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    void whenOverRateLimit_thenRejectsRequest() {
        // Mock Redis to return count over limit
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(11L));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Execute filter
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        // Verify request is rejected
        StepVerifier.create(result)
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void whenRedisFailure_thenAllowsRequest() {
        // Mock Redis failure
        when(valueOperations.increment(anyString())).thenReturn(Mono.error(new RuntimeException("Redis error")));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Execute filter
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        Mono<Void> result = filter.filter(exchange, exchange1 -> Mono.empty());

        // Verify request is allowed (fail-open)
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }
}