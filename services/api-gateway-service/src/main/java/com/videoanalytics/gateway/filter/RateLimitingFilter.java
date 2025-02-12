/**
 * Rate Limiting Filter Implementation
 * Location: src/main/java/com/videoanalytics/gateway/filter/RateLimitingFilter.java
 */
package com.videoanalytics.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    public RateLimitingFilter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = extractClientId(exchange);
            String key = RATE_LIMIT_KEY_PREFIX + clientId;

            return redisTemplate.opsForValue()
                    .increment(key)
                    .defaultIfEmpty(1L)
                    .flatMap(count -> {
                        // Set expiry on first request
                        if (count == 1) {
                            return redisTemplate.expire(key, Duration.ofSeconds(config.getPerSeconds()))
                                    .thenReturn(count);
                        }
                        return Mono.just(count);
                    })
                    .flatMap(count -> {
                        if (count > config.getMaxRequests()) {
                            return Mono.error(new ResponseStatusException(
                                    HttpStatus.TOO_MANY_REQUESTS,
                                    "Rate limit exceeded. Try again later."));
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(Exception.class, e -> {
                        if (e instanceof ResponseStatusException) {
                            return Mono.error(e);
                        }
                        // Fail open on Redis errors
                        return chain.filter(exchange);
                    });
        };
    }

    private String extractClientId(ServerWebExchange exchange) {
        // Use IP address as client identifier
        // In production, you might want to use JWT subject or API key
        return exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
    }

    public static class Config {
        private int maxRequests = 100;
        private int perSeconds = 60;

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getPerSeconds() {
            return perSeconds;
        }

        public void setPerSeconds(int perSeconds) {
            this.perSeconds = perSeconds;
        }
    }
}