/**
 * Video Like Service Interface
 * Location: src/main/java/com/videoanalytics/video/service/VideoLikeService.java
 */
package com.videoanalytics.video.service;

import com.videoanalytics.video.model.VideoLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoLikeService {
    // Like operations
    void addLike(Long videoId, Long userId);
    void removeLike(Long videoId, Long userId);
    boolean hasUserLiked(Long videoId, Long userId);

    // Analytics
    long getLikeCount(Long videoId);
    List<Long> getMostLikedVideos(LocalDateTime since, int limit);
    Page<VideoLike> getUserLikes(Long userId, Pageable pageable);
}