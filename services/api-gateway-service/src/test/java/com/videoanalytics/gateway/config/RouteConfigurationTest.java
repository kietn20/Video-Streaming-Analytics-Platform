/**
 * API Gateway Route Configuration Tests
 * Location: src/test/java/com/videoanalytics/gateway/config/RouteConfigurationTest.java
 */
package com.videoanalytics.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RouteConfigurationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void verifyAuthServiceRoute() {
        // Get all routes
        List<Route> routes = new ArrayList<>();
        StepVerifier.create(routeLocator.getRoutes())
                .recordWith(() -> routes)
                .thenConsumeWhile(x -> true)
                .verifyComplete();

        // Find auth service route
        Route authRoute = routes.stream()
                .filter(r -> r.getId().equals("auth-service"))
                .findFirst()
                .orElseThrow();

        // Verify route properties
        assertThat(authRoute.getUri().toString()).contains("auth-service");
        assertThat(authRoute.getPredicate().toString()).contains("/api/auth");

        // Verify rate limiting configuration
        assertThat(authRoute.getFilters()).anyMatch(filter ->
                filter.getClass().getSimpleName().contains("RequestRateLimiter"));
    }

    @Test
    void verifyVideoServiceRoute() {
        List<Route> routes = new ArrayList<>();
        StepVerifier.create(routeLocator.getRoutes())
                .recordWith(() -> routes)
                .thenConsumeWhile(x -> true)
                .verifyComplete();

        Route videoRoute = routes.stream()
                .filter(r -> r.getId().equals("video-service"))
                .findFirst()
                .orElseThrow();

        // Verify route properties
        assertThat(videoRoute.getUri().toString()).contains("video-service");
        assertThat(videoRoute.getPredicate().toString()).contains("/api/videos");

        // Verify JWT and rate limiting filters
        assertThat(videoRoute.getFilters()).anyMatch(filter ->
                filter.getClass().getSimpleName().contains("JwtAuthentication"));
        assertThat(videoRoute.getFilters()).anyMatch(filter ->
                filter.getClass().getSimpleName().contains("RequestRateLimiter"));
    }

    @Test
    void verifyAnalyticsServiceRoute() {
        List<Route> routes = new ArrayList<>();
        StepVerifier.create(routeLocator.getRoutes())
                .recordWith(() -> routes)
                .thenConsumeWhile(x -> true)
                .verifyComplete();

        Route analyticsRoute = routes.stream()
                .filter(r -> r.getId().equals("analytics-service"))
                .findFirst()
                .orElseThrow();

        // Verify route properties
        assertThat(analyticsRoute.getUri().toString()).contains("analytics-service");
        assertThat(analyticsRoute.getPredicate().toString()).contains("/api/analytics");

        // Verify JWT and rate limiting filters
        assertThat(analyticsRoute.getFilters()).anyMatch(filter ->
                filter.getClass().getSimpleName().contains("JwtAuthentication"));
        assertThat(analyticsRoute.getFilters()).anyMatch(filter ->
                filter.getClass().getSimpleName().contains("RequestRateLimiter"));
    }

    @Test
    void verifyRouteOrderAndPriority() {
        List<Route> routes = new ArrayList<>();
        StepVerifier.create(routeLocator.getRoutes())
                .recordWith(() -> routes)
                .thenConsumeWhile(x -> true)
                .verifyComplete();

        // Verify we have all three routes
        assertThat(routes).hasSize(3);

        // Verify auth route has highest priority (lowest order number)
        Route authRoute = routes.stream()
                .filter(r -> r.getId().equals("auth-service"))
                .findFirst()
                .orElseThrow();

        assertThat(authRoute.getOrder())
                .isLessThanOrEqualTo(routes.stream()
                        .filter(r -> !r.getId().equals("auth-service"))
                        .mapToInt(Route::getOrder)
                        .min()
                        .orElse(Integer.MAX_VALUE));
    }
}