/**
 * Video Repository Interface
 * Location: src/main/java/com/videoanalytics/video/repository/VideoRepository.java
 *
 * This interface defines the data access patterns for video entities. It extends JpaRepository
 * for basic CRUD operations and adds custom methods for specific business requirements.
 */
package com.videoanalytics.video.repository;

import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.model.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    // Basic query methods
    Optional<Video> findByStorageKey(String storageKey);
    List<Video> findByStatus(VideoStatus status);
    Page<Video> findByUploadedBy(Long userId, Pageable pageable);

    // Search methods
    Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Video> findByTagsIn(Set<String> tags, Pageable pageable);

    // Analytics queries
    @Query("SELECT v FROM Video v WHERE v.viewCount >= :minViews AND v.createdAt >= :since")
    List<Video> findTrendingVideos(Long minViews, LocalDateTime since);

    // Update operations
    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + 1 WHERE v.id = :videoId")
    void incrementLikeCount(Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount - 1 WHERE v.id = :videoId AND v.likeCount > 0")
    void decrementLikeCount(Long videoId);
}