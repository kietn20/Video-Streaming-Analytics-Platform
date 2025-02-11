package com.videoanalytics.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Profile-specific configuration
 * Location: src/main/java/com/videoanalytics/gateway/config/ProfileConfig.java
 */
@Configuration
public class ProfileConfig {

    @Profile("dev")
    @Configuration
    public static class DevConfig {
        // Development-specific beans
    }

    @Profile("test")
    @Configuration
    public static class TestConfig {
        // Test-specific beans
    }

    @Profile("prod")
    @Configuration
    public static class ProdConfig {
        // Production-specific beans
    }
}