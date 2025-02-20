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

}