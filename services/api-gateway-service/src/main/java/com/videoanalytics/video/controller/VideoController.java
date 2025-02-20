/**
 * Video Controller
 * Location: src/main/java/com/videoanalytics/video/controller/VideoController.java
 *
 * This controller provides the REST API endpoints for managing videos, including
 * uploading, retrieving, updating, and deleting videos, as well as searching and filtering.
 */
package com.videoanalytics.video.controller;

import com.videoanalytics.video.dto.VideoResponse;
import com.videoanalytics.video.dto.VideoUploadRequest;
import com.videoanalytics.video.dto.VideoUpdateRequest;
import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.model.VideoStatus;
import com.videoanalytics.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@Tag(name = "Videos", description = "Video management operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;

    /**
     * Upload a new video
     *
     * This endpoint allows users to upload video metadata. The actual video content
     * should be uploaded separately to a storage service, and the storageKey provided
     * in this request.
     */
    @PostMapping
    @Operation(
            summary = "Upload a new video",
            description = "Creates a new video entry with the provided metadata. The actual video " +
                    "content should be uploaded separately, and the storageKey provided in this request."
    )
    @ApiResponse(responseCode = "201", description = "Video successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideoResponse> uploadVideo(
            @Valid @RequestBody VideoUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received request to upload video: {}", request.getTitle());

        // Set the authenticated user as the uploader
        Long userId = extractUserId(userDetails);
        request.setUploadedBy(userId);

        // Call service to create the video
        Video createdVideo = videoService.uploadVideo(request);

        // Convert to response DTO
        VideoResponse response = convertToVideoResponse(createdVideo);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get a video by ID
     *
     * Retrieves detailed information about a specific video.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get video by ID",
            description = "Retrieves detailed information about a specific video"
    )
    @ApiResponse(responseCode = "200", description = "Video found")
    @ApiResponse(responseCode = "404", description = "Video not found")
    public ResponseEntity<VideoResponse> getVideoById(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long id) {

        log.debug("Fetching video with ID: {}", id);

        return videoService.getVideo(id)
                .map(this::convertToVideoResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a video
     *
     * Allows users to update metadata for an existing video.
     * Only the video owner or an admin can update a video.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update video metadata",
            description = "Updates an existing video's metadata. Only the video owner or an admin can perform this operation."
    )
    @ApiResponse(responseCode = "200", description = "Video successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Not authorized to update this video")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("@videoSecurityService.isVideoOwnerOrAdmin(#id, principal)")
    public ResponseEntity<VideoResponse> updateVideo(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody VideoUpdateRequest request) {

        log.info("Updating video with ID: {}", id);

        Video updatedVideo = videoService.updateVideo(id, request);
        VideoResponse response = convertToVideoResponse(updatedVideo);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a video
     *
     * Marks a video as deleted (soft delete).
     * Only the video owner or an admin can delete a video.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a video",
            description = "Marks a video as deleted (soft delete). Only the video owner or an admin can delete a video."
    )
    @ApiResponse(responseCode = "204", description = "Video successfully deleted")
    @ApiResponse(responseCode = "403", description = "Not authorized to delete this video")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("@videoSecurityService.isVideoOwnerOrAdmin(#id, principal)")
    public ResponseEntity<Void> deleteVideo(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long id) {

        log.info("Deleting video with ID: {}", id);

        videoService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search videos
     *
     * Searches for videos by title (case-insensitive partial match).
     */
    @GetMapping("/search")
    @Operation(
            summary = "Search videos",
            description = "Searches for videos by title (case-insensitive partial match)"
    )
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<Page<VideoResponse>> searchVideos(
            @Parameter(description = "Search query")
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Searching videos with query: {}", query);

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // Perform search
        Page<Video> videos = videoService.searchVideos(query, pageRequest);

        // Map to response DTOs
        Page<VideoResponse> response = videos.map(this::convertToVideoResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Filter videos by tags
     *
     * Retrieves videos that match any of the provided tags.
     */
    @GetMapping("/tags")
    @Operation(
            summary = "Filter videos by tags",
            description = "Retrieves videos that match any of the provided tags"
    )
    @ApiResponse(responseCode = "200", description = "Filtered videos")
    public ResponseEntity<Page<VideoResponse>> getVideosByTags(
            @Parameter(description = "Tags to filter by (comma-separated)")
            @RequestParam String tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Parse comma-separated tags
        Set<String> tagSet = Set.of(tags.split(","));
        log.debug("Filtering videos by tags: {}", tagSet);

        // Create pageable
        PageRequest pageRequest = PageRequest.of(page, size);

        // Perform filtering
        Page<Video> videos = videoService.findByTags(tagSet, pageRequest);

        // Map to response DTOs
        Page<VideoResponse> response = videos.map(this::convertToVideoResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's videos
     *
     * Retrieves videos uploaded by a specific user.
     */
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user's videos",
            description = "Retrieves videos uploaded by a specific user"
    )
    @ApiResponse(responseCode = "200", description = "User's videos")
    public ResponseEntity<Page<VideoResponse>> getUserVideos(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Fetching videos for user ID: {}", userId);

        // Create pageable
        PageRequest pageRequest = PageRequest.of(page, size);

        // Get user videos
        Page<Video> videos = videoService.getUserVideos(userId, pageRequest);

        // Map to response DTOs
        Page<VideoResponse> response = videos.map(this::convertToVideoResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Update video status
     *
     * Administrative endpoint to update the status of a video.
     * Only admins can access this endpoint.
     */
    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update video status",
            description = "Administrative endpoint to update the status of a video. Only admins can access this endpoint."
    )
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status")
    @ApiResponse(responseCode = "403", description = "Not authorized")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VideoResponse> updateVideoStatus(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "New status", required = true)
            @RequestParam VideoStatus status) {

        log.info("Updating status to {} for video ID: {}", status, id);

        videoService.updateVideoStatus(id, status);

        return videoService.getVideo(id)
                .map(this::convertToVideoResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Record video view
     *
     * Increments the view count for a video.
     */
    @PostMapping("/{id}/view")
    @Operation(
            summary = "Record video view",
            description = "Increments the view count for a video"
    )
    @ApiResponse(responseCode = "204", description = "View recorded successfully")
    @ApiResponse(responseCode = "404", description = "Video not found")
    public ResponseEntity<Void> recordView(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long id) {

        log.debug("Recording view for video ID: {}", id);

        videoService.recordView(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get video view count
     *
     * Retrieves the current view count for a video.
     */
    @GetMapping("/{id}/views")
    @Operation(
            summary = "Get video view count",
            description = "Retrieves the current view count for a video"
    )
    @ApiResponse(responseCode = "200", description = "View count retrieved")
    @ApiResponse(responseCode = "404", description = "Video not found")
    public ResponseEntity<Long> getViewCount(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long id) {

        log.debug("Getting view count for video ID: {}", id);

        long viewCount = videoService.getViewCount(id);
        return ResponseEntity.ok(viewCount);
    }

    // Helper methods

    /**
     * Extracts the user ID from the authenticated user details
     */
    private Long extractUserId(UserDetails userDetails) {
        // In a real application, this would extract the user ID from the UserDetails object
        // For this example, we'll assume the username is the user ID as a string
        return Long.parseLong(userDetails.getUsername());
    }

    /**
     * Converts a Video entity to a VideoResponse DTO
     */
    private VideoResponse convertToVideoResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .storageKey(video.getStorageKey())
                .duration(video.getDuration())
                .status(video.getStatus())
                .mimeType(video.getMimeType())
                .resolutionWidth(video.getResolutionWidth())
                .resolutionHeight(video.getResolutionHeight())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .tags(video.getTags())
                .uploadedBy(video.getUploadedBy())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }
}