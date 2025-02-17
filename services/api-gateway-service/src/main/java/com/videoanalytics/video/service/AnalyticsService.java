/**
 * Analytics Service Interface
 * Location: src/main/java/com/videoanalytics/video/service/AnalyticsService.java
 */
package com.videoanalytics.video.service;

import com.videoanalytics.video.dto.VideoAnalytics;
import com.videoanalytics.video.dto.UserEngagement;
import com.videoanalytics.video.dto.TrendingVideos;

import java.time.LocalDateTime;

public interface AnalyticsService {
    // Video analytics
    VideoAnalytics getVideoAnalytics(Long videoId);
    VideoAnalytics getVideoAnalyticsForPeriod(Long videoId, LocalDateTime start, LocalDateTime end);

    // User analytics
    UserEngagement getUserEngagement(Long userId);
    UserEngagement getUserEngagementForPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    // Trend analysis
    TrendingVideos getTrendingVideos(int limit);
    Map<String, Double> getEngagementMetrics(Long videoId);

    // Performance metrics
    Map<String, Double> getPerformanceMetrics(Long videoId);
    double getAverageBufferRate(Long videoId);
}