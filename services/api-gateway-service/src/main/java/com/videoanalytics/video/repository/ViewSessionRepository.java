/**
 * View Session Repository Interface
 * Location: src/main/java/com/videoanalytics/video/repository/ViewSessionRepository.java
 *
 * This interface manages viewing session data, which is crucial for tracking user engagement
 * and generating analytics. It includes methods for both session management and analytics queries.
 */
package com.videoanalytics.video.repository;

import com.videoanalytics.video.model.ViewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViewSessionRepository extends JpaRepository<ViewSession, Long> {
    // User history queries
    Page<ViewSession> findByUserId(Long userId, Pageable pageable);
    List<ViewSession> findByVideoIdAndUserId(Long videoId, Long userId);

    // Analytics queries
    @Query("SELECT vs FROM ViewSession vs WHERE vs.startedAt >= :startDate AND vs.endedAt <= :endDate")
    List<ViewSession> findSessionsInTimeRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(vs) FROM ViewSession vs WHERE vs.videoId = :videoId AND vs.watchDuration >= :minDuration")
    Long countCompletedViews(Long videoId, Duration minDuration);

    @Query("SELECT vs.deviceType, COUNT(vs) FROM ViewSession vs " +
            "WHERE vs.videoId = :videoId GROUP BY vs.deviceType")
    List<Object[]> getDeviceTypeDistribution(Long videoId);

    // Performance metrics
    @Query("SELECT AVG(vs.bufferEvents) FROM ViewSession vs " +
            "WHERE vs.videoId = :videoId AND vs.endedAt >= :since")
    Double getAverageBufferEvents(Long videoId, LocalDateTime since);
}