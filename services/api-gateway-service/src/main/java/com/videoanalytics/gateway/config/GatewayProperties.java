package com.videoanalytics.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the API Gateway
 * Location: src/main/java/com/videoanalytics/gateway/config/GatewayProperties.java
 */
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();

    // Nested JWT configuration class
    public static class Jwt {
        private String secret;
        private long expirationMs;

        // Getters and setters
    }

    // Nested rate limit configuration class
    public static class RateLimit {
        private int maxRequests;
        private int perSeconds;

        // Getters and setters
    }

    // Getters and setters
}