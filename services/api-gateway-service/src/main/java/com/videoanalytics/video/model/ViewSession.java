/**
 * View Session Entity
 * Location: src/main/java/com/videoanalytics/video/model/ViewSession.java
 *
 * This class tracks individual viewing sessions of videos. It records when users
 * start and stop watching videos, along with their viewing progress. This data
 * is crucial for analytics and recommendations.
 */
package com.videoanalytics.video.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "view_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ViewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Session timing information
    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // Viewing progress
    @Column(name = "watch_duration")
    private Duration watchDuration;

    @Column(name = "last_position")
    private Duration lastPosition;

    // Device and platform information
    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "platform")
    private String platform;

    @Column(name = "ip_address")
    private String ipAddress;

    // Quality metrics
    @Column(name = "quality_switches")
    private Integer qualitySwitches = 0;

    @Column(name = "buffer_events")
    private Integer bufferEvents = 0;

    @Column(name = "average_bitrate")
    private Long averageBitrate;

    public ViewSession(Video video, Long userId, String deviceType, String platform, String ipAddress) {
        this.video = video;
        this.userId = userId;
        this.deviceType = deviceType;
        this.platform = platform;
        this.ipAddress = ipAddress;
    }

    public void endSession(Duration watchDuration, Duration lastPosition) {
        this.endedAt = LocalDateTime.now();
        this.watchDuration = watchDuration;
        this.lastPosition = lastPosition;
    }

    public void recordQualitySwitch() {
        this.qualitySwitches++;
    }

    public void recordBufferEvent() {
        this.bufferEvents++;
    }
}