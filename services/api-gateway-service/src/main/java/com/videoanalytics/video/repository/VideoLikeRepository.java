/**
 * Video Like Repository Interface
 * Location: src/main/java/com/videoanalytics/video/repository/VideoLikeRepository.java
 *
 * This interface handles user interactions with videos in the form of likes.
 * It includes methods for managing likes and retrieving engagement metrics.
 */
package com.videoanalytics.video.repository;

import com.videoanalytics.video.model.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {
    // Basic operations
    Optional<VideoLike> findByVideoIdAndUserId(Long videoId, Long userId);
    boolean existsByVideoIdAndUserId(Long videoId, Long userId);
    void deleteByVideoIdAndUserId(Long videoId, Long userId);

    // Analytics queries
    @Query("SELECT COUNT(vl) FROM VideoLike vl WHERE vl.video.id = :videoId")
    Long countLikesByVideoId(Long videoId);

    @Query("SELECT vl.video.id, COUNT(vl) FROM VideoLike vl " +
            "WHERE vl.createdAt >= :since GROUP BY vl.video.id " +
            "ORDER BY COUNT(vl) DESC")
    List<Object[]> findMostLikedVideos(LocalDateTime since);

    @Query("SELECT COUNT(vl) FROM VideoLike vl " +
            "WHERE vl.userId = :userId AND vl.createdAt >= :since")
    Long countUserLikesSince(Long userId, LocalDateTime since);
}