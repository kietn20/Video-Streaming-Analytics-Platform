/**
 * Video Entity
 * Location: src/main/java/com/videoanalytics/video/model/Video.java
 *
 * This class represents the core video metadata in our system. It contains
 * essential information about each video, including its basic attributes,
 * upload details, and current status.
 */
package com.videoanalytics.video.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    // Unique identifier for the video file in storage
    @NotBlank
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    // Duration of the video in seconds
    @Column(nullable = false)
    private Duration duration;

    // Video status (PROCESSING, READY, ERROR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.PROCESSING;

    // Technical metadata
    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "resolution_width")
    private Integer resolutionWidth;

    @Column(name = "resolution_height")
    private Integer resolutionHeight;

    // Tracking metadata
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    // Views and engagement metrics
    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    // Tags for categorization and search
    @ElementCollection
    @CollectionTable(
            name = "video_tags",
            joinColumns = @JoinColumn(name = "video_id")
    )
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    public Video(String title, String storageKey, Duration duration, Long uploadedBy) {
        this.title = title;
        this.storageKey = storageKey;
        this.duration = duration;
        this.uploadedBy = uploadedBy;
    }

    // Increment view count atomically
    public synchronized void incrementViewCount() {
        this.viewCount++;
    }

    // Increment like count atomically
    public synchronized void incrementLikeCount() {
        this.likeCount++;
    }

    // Decrement like count atomically
    public synchronized void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}