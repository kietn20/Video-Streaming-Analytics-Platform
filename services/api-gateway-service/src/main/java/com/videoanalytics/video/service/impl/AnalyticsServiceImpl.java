/**
 * Analytics Service Implementation
 * Location: src/main/java/com/videoanalytics/video/service/impl/AnalyticsServiceImpl.java
 */
package com.videoanalytics.video.service.impl;

import com.videoanalytics.video.dto.VideoAnalytics;
import com.videoanalytics.video.dto.UserEngagement;
import com.videoanalytics.video.dto.TrendingVideos;
import com.videoanalytics.video.exception.VideoNotFoundException;
import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.model.ViewSession;
import com.videoanalytics.video.repository.VideoRepository;
import com.videoanalytics.video.repository.VideoLikeRepository;
import com.videoanalytics.video.repository.ViewSessionRepository;
import com.videoanalytics.video.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final ViewSessionRepository viewSessionRepository;

    // Cache duration for analytics data (15 minutes)
    private static final Duration CACHE_DURATION = Duration.ofMinutes(15);

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "videoAnalytics", key = "#videoId")
    public VideoAnalytics getVideoAnalytics(Long videoId) {
        log.info("Generating analytics for video ID: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + videoId));

        // Get view sessions for the video
        List<ViewSession> sessions = viewSessionRepository.findSessionsInTimeRange(
                        LocalDateTime.now().minusMonths(1),
                        LocalDateTime.now()
                ).stream()
                .filter(session -> videoId.equals(session.getVideo().getId()))
                .toList();

        return VideoAnalytics.builder()
                .videoId(videoId)
                .title(video.getTitle())
                .totalViews(video.getViewCount())
                .totalLikes(video.getLikeCount())
                .averageWatchDuration(calculateAverageWatchDuration(sessions))
                .completionRate(calculateCompletionRate(sessions, video.getDuration()))
                .deviceDistribution(calculateDeviceDistribution(sessions))
                .qualityMetrics(calculateQualityMetrics(sessions))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VideoAnalytics getVideoAnalyticsForPeriod(Long videoId, LocalDateTime start, LocalDateTime end) {
        log.info("Generating period analytics for video ID: {} from {} to {}", videoId, start, end);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + videoId));

        List<ViewSession> sessions = viewSessionRepository.findSessionsInTimeRange(start, end)
                .stream()
                .filter(session -> videoId.equals(session.getVideo().getId()))
                .toList();

        return VideoAnalytics.builder()
                .videoId(videoId)
                .title(video.getTitle())
                .totalViews(sessions.size())
                .totalLikes(calculatePeriodLikes(videoId, start, end))
                .averageWatchDuration(calculateAverageWatchDuration(sessions))
                .completionRate(calculateCompletionRate(sessions, video.getDuration()))
                .deviceDistribution(calculateDeviceDistribution(sessions))
                .qualityMetrics(calculateQualityMetrics(sessions))
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userEngagement", key = "#userId")
    public UserEngagement getUserEngagement(Long userId) {
        log.info("Generating engagement metrics for user ID: {}", userId);

        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        return UserEngagement.builder()
                .userId(userId)
                .totalWatchTime(calculateTotalWatchTime(userId, monthAgo))
                .videosWatched(countVideosWatched(userId, monthAgo))
                .averageWatchDuration(calculateUserAverageWatchDuration(userId, monthAgo))
                .totalLikes(countUserLikes(userId, monthAgo))
                .devicePreferences(getUserDevicePreferences(userId, monthAgo))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserEngagement getUserEngagementForPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        log.info("Generating period engagement for user ID: {} from {} to {}", userId, start, end);

        return UserEngagement.builder()
                .userId(userId)
                .totalWatchTime(calculateTotalWatchTime(userId, start, end))
                .videosWatched(countVideosWatched(userId, start, end))
                .averageWatchDuration(calculateUserAverageWatchDuration(userId, start, end))
                .totalLikes(countUserLikes(userId, start, end))
                .devicePreferences(getUserDevicePreferences(userId, start, end))
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "trendingVideos")
    public TrendingVideos getTrendingVideos(int limit) {
        log.info("Getting trending videos with limit: {}", limit);

        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);

        // Get videos with high view counts
        List<Video> trendingByViews = videoRepository.findTrendingVideos(100L, weekAgo);

        // Get most liked videos
        List<Long> trendingByLikes = videoLikeRepository.findMostLikedVideos(weekAgo)
                .stream()
                .map(row -> (Long) row[0])
                .limit(limit)
                .toList();

        return TrendingVideos.builder()
                .topByViews(trendingByViews.stream().limit(limit).collect(Collectors.toList()))
                .topByLikes(trendingByLikes)
                .periodStart(weekAgo)
                .periodEnd(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getEngagementMetrics(Long videoId) {
        log.info("Calculating engagement metrics for video ID: {}", videoId);

        Map<String, Double> metrics = new HashMap<>();

        // Get recent sessions
        List<ViewSession> sessions = viewSessionRepository.findSessionsInTimeRange(
                        LocalDateTime.now().minusMonths(1),
                        LocalDateTime.now()
                ).stream()
                .filter(session -> videoId.equals(session.getVideo().getId()))
                .toList();

        metrics.put("averageWatchDuration", calculateAverageWatchDuration(sessions));
        metrics.put("completionRate", calculateCompletionRate(sessions,
                videoRepository.findById(videoId)
                        .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + videoId))
                        .getDuration()));
        metrics.put("replayRate", calculateReplayRate(sessions));

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getPerformanceMetrics(Long videoId) {
        log.info("Calculating performance metrics for video ID: {}", videoId);

        Map<String, Double> metrics = new HashMap<>();

        List<ViewSession> sessions = viewSessionRepository.findSessionsInTimeRange(
                        LocalDateTime.now().minusMonths(1),
                        LocalDateTime.now()
                ).stream()
                .filter(session -> videoId.equals(session.getVideo().getId()))
                .toList();

        metrics.put("averageBufferEvents", calculateAverageBufferEvents(sessions));
        metrics.put("qualitySwitchRate", calculateQualitySwitchRate(sessions));
        metrics.put("averageBitrate", calculateAverageBitrate(sessions));

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageBufferRate(Long videoId) {
        log.info("Calculating average buffer rate for video ID: {}", videoId);

        return viewSessionRepository.getAverageBufferEvents(videoId, LocalDateTime.now().minusMonths(1));
    }

    // Helper methods for calculations

    private double calculateAverageWatchDuration(List<ViewSession> sessions) {
        if (sessions.isEmpty()) return 0.0;

        return sessions.stream()
                .filter(session -> session.getWatchDuration() != null)
                .mapToLong(session -> session.getWatchDuration().getSeconds())
                .average()
                .orElse(0.0);
    }

    private double calculateCompletionRate(List<ViewSession> sessions, Duration videoDuration) {
        if (sessions.isEmpty()) return 0.0;

        long completedViews = sessions.stream()
                .filter(session -> session.getWatchDuration() != null)
                .filter(session -> session.getWatchDuration().getSeconds() >= videoDuration.getSeconds() * 0.9)
                .count();

        return (double) completedViews / sessions.size() * 100;
    }

    private Map<String, Long> calculateDeviceDistribution(List<ViewSession> sessions) {
        return sessions.stream()
                .collect(Collectors.groupingBy(
                        ViewSession::getDeviceType,
                        Collectors.counting()
                ));
    }

    private Map<String, Double> calculateQualityMetrics(List<ViewSession> sessions) {
        Map<String, Double> metrics = new HashMap<>();

        metrics.put("averageBufferEvents", calculateAverageBufferEvents(sessions));
        metrics.put("qualitySwitchRate", calculateQualitySwitchRate(sessions));
        metrics.put("averageBitrate", calculateAverageBitrate(sessions));

        return metrics;
    }


}