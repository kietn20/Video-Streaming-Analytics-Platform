
/**
 * View Session Service Interface
 * Location: src/main/java/com/videoanalytics/video/service/ViewSessionService.java
 */
package com.videoanalytics.video.service;

import com.videoanalytics.video.model.ViewSession;
import com.videoanalytics.video.dto.ViewSessionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ViewSessionService {
    // Session management
    ViewSession startSession(ViewSessionRequest request);
    ViewSession updateSession(Long sessionId, ViewSessionRequest request);
    void endSession(Long sessionId);

    // User history
    Page<ViewSession> getUserSessions(Long userId, Pageable pageable);

    // Analytics
    Map<String, Long> getDeviceDistribution(Long videoId);
    double getAverageWatchDuration(Long videoId);
    List<ViewSession> getSessionsInTimeRange(LocalDateTime start, LocalDateTime end);
    long getCompletionRate(Long videoId);
}