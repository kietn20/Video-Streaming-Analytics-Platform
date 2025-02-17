/**
 * Video Like Service Implementation
 * Location: src/main/java/com/videoanalytics/video/service/impl/VideoLikeServiceImpl.java
 */
package com.videoanalytics.video.service.impl;

import com.videoanalytics.video.exception.DuplicateLikeException;
import com.videoanalytics.video.exception.LikeNotFoundException;
import com.videoanalytics.video.exception.VideoNotFoundException;
import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.model.VideoLike;
import com.videoanalytics.video.repository.VideoRepository;
import com.videoanalytics.video.repository.VideoLikeRepository;
import com.videoanalytics.video.service.VideoLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoLikeServiceImpl implements VideoLikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public void addLike(Long videoId, Long userId) {
        log.info("Adding like for video ID: {} by user ID: {}", videoId, userId);

        // Check if video exists
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + videoId));

        // Check if user has already liked the video
        if (videoLikeRepository.existsByVideoIdAndUserId(videoId, userId)) {
            throw new DuplicateLikeException("User has already liked this video");
        }

        try {
            // Create new like
            VideoLike like = new VideoLike(video, userId);
            videoLikeRepository.save(like);

            // Increment video like count
            videoRepository.incrementLikeCount(videoId);

            log.info("Successfully added like for video ID: {} by user ID: {}", videoId, userId);
        } catch (DataIntegrityViolationException e) {
            // Handle rare case of concurrent like addition
            log.warn("Concurrent like addition detected for video ID: {} by user ID: {}", videoId, userId);
            throw new DuplicateLikeException("Like already exists (concurrent operation)");
        }
    }

    @Override
    @Transactional
    public void removeLike(Long videoId, Long userId) {
        log.info("Removing like for video ID: {} by user ID: {}", videoId, userId);

        // Check if like exists
        VideoLike like = videoLikeRepository.findByVideoIdAndUserId(videoId, userId)
                .orElseThrow(() -> new LikeNotFoundException("Like not found for video ID: " + videoId + " and user ID: " + userId));

        // Remove like
        videoLikeRepository.delete(like);

        // Decrement video like count
        videoRepository.decrementLikeCount(videoId);

        log.info("Successfully removed like for video ID: {} by user ID: {}", videoId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserLiked(Long videoId, Long userId) {
        log.debug("Checking if user ID: {} has liked video ID: {}", userId, videoId);
        return videoLikeRepository.existsByVideoIdAndUserId(videoId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(Long videoId) {
        log.debug("Getting like count for video ID: {}", videoId);

        // Verify video exists
        if (!videoRepository.existsById(videoId)) {
            throw new VideoNotFoundException("Video not found with ID: " + videoId);
        }

        return videoLikeRepository.countLikesByVideoId(videoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMostLikedVideos(LocalDateTime since, int limit) {
        log.debug("Getting most liked videos since: {} with limit: {}", since, limit);

        // Get video IDs and like counts
        List<Object[]> results = videoLikeRepository.findMostLikedVideos(since);

        // Extract video IDs and apply limit
        return results.stream()
                .map(row -> (Long) row[0])  // First element is video ID
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoLike> getUserLikes(Long userId, Pageable pageable) {
        log.debug("Getting likes for user ID: {} with pagination", userId);
        return videoLikeRepository.findAll(pageable);
    }

    /**
     * Helper method to ensure atomic like count updates.
     * This method uses optimistic locking to handle concurrent modifications.
     */
    private void updateLikeCount(Long videoId, boolean increment) {
        log.debug("{} like count for video ID: {}", increment ? "Incrementing" : "Decrementing", videoId);

        boolean updated = false;
        int retries = 0;
        int maxRetries = 3;

        while (!updated && retries < maxRetries) {
            try {
                if (increment) {
                    videoRepository.incrementLikeCount(videoId);
                } else {
                    videoRepository.decrementLikeCount(videoId);
                }
                updated = true;
            } catch (Exception e) {
                log.warn("Retry {} failed for updating like count on video ID: {}", retries + 1, videoId);
                retries++;
                if (retries == maxRetries) {
                    throw new RuntimeException("Failed to update like count after " + maxRetries + " attempts", e);
                }
            }
        }
    }
}