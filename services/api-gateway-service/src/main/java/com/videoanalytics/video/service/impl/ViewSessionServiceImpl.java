/**
 * View Session Service Implementation
 * Location: src/main/java/com/videoanalytics/video/service/impl/ViewSessionServiceImpl.java
 */
package com.videoanalytics.video.service.impl;

import com.videoanalytics.video.exception.SessionNotFoundException;
import com.videoanalytics.video.exception.VideoNotFoundException;
import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.model.ViewSession;
import com.videoanalytics.video.repository.VideoRepository;
import com.videoanalytics.video.repository.ViewSessionRepository;
import com.videoanalytics.video.service.ViewSessionService;
import com.videoanalytics.video.dto.ViewSessionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViewSessionServiceImpl implements ViewSessionService {

    private final ViewSessionRepository viewSessionRepository;
    private final VideoRepository videoRepository;

    // Threshold for considering a video "completed" (e.g., 90% watched)
    private static final double COMPLETION_THRESHOLD = 0.9;

    @Override
    @Transactional
    public ViewSession startSession(ViewSessionRequest request) {
        log.info("Starting new view session for video ID: {}", request.getVideoId());

        // Verify video exists
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + request.getVideoId()));

        // Create new session
        ViewSession session = new ViewSession(
                video,
                request.getUserId(),
                request.getDeviceType(),
                request.getPlatform(),
                request.getIpAddress()
        );

        // Save and return the session
        ViewSession savedSession = viewSessionRepository.save(session);
        log.info("Started view session with ID: {}", savedSession.getId());
        return savedSession;
    }

    @Override
    @Transactional
    public ViewSession updateSession(Long sessionId, ViewSessionRequest request) {
        log.info("Updating view session with ID: {}", sessionId);

        // Find existing session
        ViewSession session = viewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with ID: " + sessionId));

        // Update session metrics
        if (request.getLastPosition() != null) {
            session.setLastPosition(request.getLastPosition());
        }
        if (request.getAverageBitrate() != null) {
            session.setAverageBitrate(request.getAverageBitrate());
        }

        // Record quality switch if occurred
        if (Boolean.TRUE.equals(request.getQualitySwitch())) {
            session.recordQualitySwitch();
        }

        // Record buffer event if occurred
        if (Boolean.TRUE.equals(request.getBufferEvent())) {
            session.recordBufferEvent();
        }

        // Save and return updated session
        ViewSession updatedSession = viewSessionRepository.save(session);
        log.info("Updated view session with ID: {}", updatedSession.getId());
        return updatedSession;
    }

    @Override
    @Transactional
    public void endSession(Long sessionId) {
        log.info("Ending view session with ID: {}", sessionId);

        // Find existing session
        ViewSession session = viewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with ID: " + sessionId));

        // Calculate watch duration
        Duration watchDuration = Duration.between(session.getStartedAt(), LocalDateTime.now());

        // End the session
        session.endSession(watchDuration, session.getLastPosition());
        viewSessionRepository.save(session);

        log.info("Ended view session with ID: {}", sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewSession> getUserSessions(Long userId, Pageable pageable) {
        log.debug("Fetching view sessions for user ID: {}", userId);
        return viewSessionRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDeviceDistribution(Long videoId) {
        log.debug("Getting device distribution for video ID: {}", videoId);

        // Get device distribution from repository
        List<Object[]> distribution = viewSessionRepository.getDeviceTypeDistribution(videoId);

        // Convert to Map<DeviceType, Count>
        return distribution.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (existing, replacement) -> existing    // Keep existing value in case of duplicates
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageWatchDuration(Long videoId) {
        log.debug("Calculating average watch duration for video ID: {}", videoId);

        // Get all completed sessions for the video
        List<ViewSession> sessions = viewSessionRepository.findSessionsInTimeRange(
                        LocalDateTime.now().minusMonths(1),  // Last month of data
                        LocalDateTime.now()
                ).stream()
                .filter(session -> videoId.equals(session.getVideo().getId()))
                .filter(session -> session.getWatchDuration() != null)
                .toList();

        // Calculate average duration in seconds
        if (sessions.isEmpty()) {
            return 0.0;
        }

        double totalSeconds = sessions.stream()
                .mapToLong(session -> session.getWatchDuration().getSeconds())
                .sum();

        return totalSeconds / sessions.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewSession> getSessionsInTimeRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching sessions between {} and {}", start, end);
        return viewSessionRepository.findSessionsInTimeRange(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletionRate(Long videoId) {
        log.debug("Calculating completion rate for video ID: {}", videoId);

        // Get video duration
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + videoId));

        Duration minDuration = video.getDuration().multipliedBy((long) (COMPLETION_THRESHOLD * 100)).dividedBy(100);

        // Count completed views
        return viewSessionRepository.countCompletedViews(videoId, minDuration);
    }
}