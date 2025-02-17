/**
 * Video Service Interface
 * Location: src/main/java/com/videoanalytics/video/service/VideoService.java
 */
package com.videoanalytics.video.service;

import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.dto.VideoUploadRequest;
import com.videoanalytics.video.dto.VideoUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface VideoService {
    // Core video operations
    Video uploadVideo(VideoUploadRequest request);
    Optional<Video> getVideo(Long id);
    Video updateVideo(Long id, VideoUpdateRequest request);
    void deleteVideo(Long id);

    // Search and filtering
    Page<Video> searchVideos(String query, Pageable pageable);
    Page<Video> findByTags(Set<String> tags, Pageable pageable);
    Page<Video> getUserVideos(Long userId, Pageable pageable);

    // Status management
    void updateVideoStatus(Long id, VideoStatus newStatus);

    // Analytics triggers
    void recordView(Long id);
    long getViewCount(Long id);
}