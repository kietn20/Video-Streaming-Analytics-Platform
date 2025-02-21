/**
 * Analytics Controller
 * Location: src/main/java/com/videoanalytics/video/controller/AnalyticsController.java
 *
 * This controller provides comprehensive analytics endpoints that integrate
 * data from various services to offer insights about video performance,
 * user engagement, and platform usage.
 */
package com.videoanalytics.video.controller;

import com.videoanalytics.video.dto.TrendingVideos;
import com.videoanalytics.video.dto.UserEngagement;
import com.videoanalytics.video.dto.VideoAnalytics;
import com.videoanalytics.video.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Comprehensive analytics for videos, users, and platform usage")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get comprehensive analytics for a specific video
     *
     * Provides detailed performance metrics for a video including views, likes,
     * watch time, completion rates, and device distribution.
     */
    @GetMapping("/videos/{videoId}")
    @Operation(
            summary = "Get video analytics",
            description = "Provides detailed performance metrics for a video including views, likes, " +
                    "watch time, completion rates, and device distribution."
    )
    @ApiResponse(responseCode = "200", description = "Analytics successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view these analytics")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<VideoAnalytics> getVideoAnalytics(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.info("Retrieving analytics for video ID: {}", videoId);

        VideoAnalytics analytics = analyticsService.getVideoAnalytics(videoId);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get period-specific analytics for a video
     *
     * Retrieves analytics for a specific video within a specified date range.
     * Useful for tracking performance changes over time.
     */
    @GetMapping("/videos/{videoId}/period")
    @Operation(
            summary = "Get period-specific video analytics",
            description = "Retrieves analytics for a specific video within a specified date range. " +
                    "Useful for tracking performance changes over time."
    )
    @ApiResponse(responseCode = "200", description = "Period analytics successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view these analytics")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<VideoAnalytics> getVideoAnalyticsForPeriod(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId,
            @Parameter(description = "Start date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Retrieving period analytics for video ID: {} from {} to {}", videoId, start, end);

        VideoAnalytics analytics = analyticsService.getVideoAnalyticsForPeriod(videoId, start, end);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get user engagement metrics
     *
     * Provides insights into a user's platform usage including total watch time,
     * videos watched, and engagement patterns.
     */
    @GetMapping("/users/{userId}")
    @Operation(
            summary = "Get user engagement metrics",
            description = "Provides insights into a user's platform usage including total watch time, " +
                    "videos watched, and engagement patterns."
    )
    @ApiResponse(responseCode = "200", description = "User engagement successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view these analytics")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isSameUser(#userId, principal)")
    public ResponseEntity<UserEngagement> getUserEngagement(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {

        log.info("Retrieving engagement metrics for user ID: {}", userId);

        UserEngagement engagement = analyticsService.getUserEngagement(userId);
        return ResponseEntity.ok(engagement);
    }

    /**
     * Get period-specific user engagement
     *
     * Retrieves user engagement metrics within a specified date range.
     * Useful for tracking changes in user behavior over time.
     */
    @GetMapping("/users/{userId}/period")
    @Operation(
            summary = "Get period-specific user engagement",
            description = "Retrieves user engagement metrics within a specified date range. " +
                    "Useful for tracking changes in user behavior over time."
    )
    @ApiResponse(responseCode = "200", description = "Period engagement successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view these analytics")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isSameUser(#userId, principal)")
    public ResponseEntity<UserEngagement> getUserEngagementForPeriod(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Start date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Retrieving period engagement for user ID: {} from {} to {}", userId, start, end);

        UserEngagement engagement = analyticsService.getUserEngagementForPeriod(userId, start, end);
        return ResponseEntity.ok(engagement);
    }

    /**
     * Get trending videos
     *
     * Identifies the most popular videos based on views and likes over recent time periods.
     * Useful for content discovery and recommendations.
     */
    @GetMapping("/trending")
    @Operation(
            summary = "Get trending videos",
            description = "Identifies the most popular videos based on views and likes over recent time periods. " +
                    "Useful for content discovery and recommendations."
    )
    @ApiResponse(responseCode = "200", description = "Trending videos successfully retrieved")
    public ResponseEntity<TrendingVideos> getTrendingVideos(
            @Parameter(description = "Maximum number of videos to return")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Retrieving trending videos with limit: {}", limit);

        TrendingVideos trending = analyticsService.getTrendingVideos(limit);
        return ResponseEntity.ok(trending);
    }

    /**
     * Get engagement metrics for a video
     *
     * Provides detailed engagement metrics for a specific video, focusing on
     * user interaction patterns.
     */
    @GetMapping("/engagement/{videoId}")
    @Operation(
            summary = "Get engagement metrics for a video",
            description = "Provides detailed engagement metrics for a specific video, focusing on " +
                    "user interaction patterns."
    )
    @ApiResponse(responseCode = "200", description = "Engagement metrics successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view these metrics")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<Map<String, Double>> getEngagementMetrics(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.info("Retrieving engagement metrics for video ID: {}", videoId);

        Map<String, Double> metrics = analyticsService.getEngagementMetrics(videoId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get performance metrics for a video
     *
     * Provides technical performance metrics for a specific video, focusing on
     * playback quality and user experience.
     */
    @GetMapping("/performance/{videoId}")
    @Operation(
            summary = "Get performance metrics for a video",
            description = "Provides technical performance metrics for a specific video, focusing on " +
                    "playback quality and user experience."
    )
    @ApiResponse(responseCode = "200", description = "Performance metrics successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view these metrics")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<Map<String, Double>> getPerformanceMetrics(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.info("Retrieving performance metrics for video ID: {}", videoId);

        Map<String, Double> metrics = analyticsService.getPerformanceMetrics(videoId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get platform-wide metrics dashboard
     *
     * Administrative endpoint that provides a comprehensive view of platform usage,
     * performance, and engagement. Only available to administrators.
     */
    @GetMapping("/dashboard")
    @Operation(
            summary = "Get platform-wide metrics dashboard",
            description = "Administrative endpoint that provides a comprehensive view of platform usage, " +
                    "performance, and engagement. Only available to administrators."
    )
    @ApiResponse(responseCode = "200", description = "Dashboard metrics successfully retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view platform dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPlatformDashboard() {
        log.info("Retrieving platform-wide analytics dashboard");

        // This could be implemented in the AnalyticsService to provide
        // a comprehensive view of platform metrics

        return ResponseEntity.ok(Map.of(
                "message", "Platform dashboard endpoint - to be implemented"
        ));
    }
}