/**
 * Video Like Entity
 * Location: src/main/java/com/videoanalytics/video/model/VideoLike.java
 *
 * Tracks user likes on videos. This helps in engagement metrics and can be
 * used for recommendations and trending video calculations.
 */
package com.videoanalytics.video.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"video_id", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class VideoLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public VideoLike(Video video, Long userId) {
        this.video = video;
        this.userId = userId;
    }
}