/**
 * Video Like Controller
 * Location: src/main/java/com/videoanalytics/video/controller/VideoLikeController.java
 *
 * This controller provides REST API endpoints for managing video likes,
 * including adding and removing likes, checking if a user has liked a video,
 * and retrieving like-related analytics.
 */
package com.videoanalytics.video.controller;

import com.videoanalytics.video.dto.VideoLikeResponse;
import com.videoanalytics.video.model.VideoLike;
import com.videoanalytics.video.service.VideoLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/likes")
@Tag(name = "Video Likes", description = "Operations for managing video likes and engagement")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class VideoLikeController {

    private final VideoLikeService videoLikeService;

    /**
     * Add a like to a video
     * <p>
     * Creates a new like record for the specified video by the authenticated user.
     * A user can only like a video once.
     */
    @PostMapping("/{videoId}")
    @Operation(
            summary = "Like a video",
            description = "Creates a new like for the specified video by the authenticated user. " +
                    "A user can only like a video once."
    )
    @ApiResponse(responseCode = "204", description = "Like successfully added")
    @ApiResponse(responseCode = "400", description = "User has already liked this video")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> likeVideo(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User liking video with ID: {}", videoId);

        // Extract user ID from authenticated user
        Long userId = extractUserId(userDetails);

        // Add the like
        videoLikeService.addLike(videoId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a like from a video
     * <p>
     * Deletes the like record for the specified video by the authenticated user.
     * The user must have previously liked the video.
     */
    @DeleteMapping("/{videoId}")
    @Operation(
            summary = "Unlike a video",
            description = "Removes the like for the specified video by the authenticated user. " +
                    "The user must have previously liked the video."
    )
    @ApiResponse(responseCode = "204", description = "Like successfully removed")
    @ApiResponse(responseCode = "404", description = "Like not found or video not found")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlikeVideo(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User unliking video with ID: {}", videoId);

        // Extract user ID from authenticated user
        Long userId = extractUserId(userDetails);

        // Remove the like
        videoLikeService.removeLike(videoId, userId);

        return ResponseEntity.noContent().build();
    }

}