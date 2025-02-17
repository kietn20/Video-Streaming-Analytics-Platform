/**
 * Video Service Implementation
 * Location: src/main/java/com/videoanalytics/video/service/impl/VideoServiceImpl.java
 */
package com.videoanalytics.video.service.impl;

import com.videoanalytics.video.exception.VideoNotFoundException;
import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.model.VideoStatus;
import com.videoanalytics.video.repository.VideoRepository;
import com.videoanalytics.video.service.VideoService;
import com.videoanalytics.video.dto.VideoUploadRequest;
import com.videoanalytics.video.dto.VideoUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public Video uploadVideo(VideoUploadRequest request) {
        log.info("Uploading new video: {}", request.getTitle());

        // Create new video entity from request
        Video video = new Video(
                request.getTitle(),
                request.getStorageKey(),
                request.getDuration(),
                request.getUploadedBy()
        );

        // Set additional metadata
        video.setDescription(request.getDescription());
        video.setMimeType(request.getMimeType());
        video.setFileSize(request.getFileSize());
        video.setResolutionWidth(request.getResolutionWidth());
        video.setResolutionHeight(request.getResolutionHeight());
        video.setTags(request.getTags());

        // Set initial status as PROCESSING
        video.setStatus(VideoStatus.PROCESSING);

        // Save and return the video
        Video savedVideo = videoRepository.save(video);
        log.info("Successfully uploaded video with ID: {}", savedVideo.getId());
        return savedVideo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Video> getVideo(Long id) {
        log.debug("Fetching video with ID: {}", id);
        return videoRepository.findById(id);
    }

    @Override
    @Transactional
    public Video updateVideo(Long id, VideoUpdateRequest request) {
        log.info("Updating video with ID: {}", id);

        // Find existing video or throw exception
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + id));

        // Update video metadata
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            video.setTags(request.getTags());
        }

        // Save and return updated video
        Video updatedVideo = videoRepository.save(video);
        log.info("Successfully updated video with ID: {}", updatedVideo.getId());
        return updatedVideo;
    }

    @Override
    @Transactional
    public void deleteVideo(Long id) {
        log.info("Deleting video with ID: {}", id);

        // Find existing video or throw exception
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + id));

        // Mark video as deleted instead of physical deletion
        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);

        log.info("Successfully marked video as deleted with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Video> searchVideos(String query, Pageable pageable) {
        log.debug("Searching videos with query: {}", query);
        return videoRepository.findByTitleContainingIgnoreCase(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Video> findByTags(Set<String> tags, Pageable pageable) {
        log.debug("Finding videos with tags: {}", tags);
        return videoRepository.findByTagsIn(tags, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Video> getUserVideos(Long userId, Pageable pageable) {
        log.debug("Fetching videos for user ID: {}", userId);
        return videoRepository.findByUploadedBy(userId, pageable);
    }

    @Override
    @Transactional
    public void updateVideoStatus(Long id, VideoStatus newStatus) {
        log.info("Updating status to {} for video ID: {}", newStatus, id);

        // Find existing video or throw exception
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + id));

        // Update status
        video.setStatus(newStatus);
        videoRepository.save(video);

        log.info("Successfully updated status for video ID: {}", id);
    }

    @Override
    @Transactional
    public void recordView(Long id) {
        log.debug("Recording view for video ID: {}", id);

        // Increment view count atomically
        videoRepository.incrementViewCount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long getViewCount(Long id) {
        log.debug("Getting view count for video ID: {}", id);

        return videoRepository.findById(id)
                .map(Video::getViewCount)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with ID: " + id));
    }
}