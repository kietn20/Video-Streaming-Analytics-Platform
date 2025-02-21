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

    /**
     * Check if user has liked a video
     *
     * Returns whether the authenticated user has liked the specified video.
     */
    @GetMapping("/{videoId}/check")
    @Operation(
            summary = "Check if user has liked a video",
            description = "Returns whether the authenticated user has liked the specified video"
    )
    @ApiResponse(responseCode = "200", description = "Like status retrieved")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> hasUserLikedVideo(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Checking if user has liked video with ID: {}", videoId);

        // Extract user ID from authenticated user
        Long userId = extractUserId(userDetails);

        // Check if user has liked the video
        boolean hasLiked = videoLikeService.hasUserLiked(videoId, userId);

        return ResponseEntity.ok(hasLiked);
    }

    /**
     * Get like count for a video
     *
     * Returns the total number of likes for the specified video.
     */
    @GetMapping("/count/{videoId}")
    @Operation(
            summary = "Get like count for a video",
            description = "Returns the total number of likes for the specified video"
    )
    @ApiResponse(responseCode = "200", description = "Like count retrieved")
    @ApiResponse(responseCode = "404", description = "Video not found")
    public ResponseEntity<Long> getLikeCount(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.debug("Getting like count for video with ID: {}", videoId);

        long likeCount = videoLikeService.getLikeCount(videoId);

        return ResponseEntity.ok(likeCount);
    }

    /**
     * Get most liked videos
     *
     * Returns a list of the most liked videos since the specified date.
     */
    @GetMapping("/trending")
    @Operation(
            summary = "Get most liked videos",
            description = "Returns a list of the most liked videos since the specified date"
    )
    @ApiResponse(responseCode = "200", description = "Trending videos retrieved")
    public ResponseEntity<List<Long>> getMostLikedVideos(
            @Parameter(description = "Start date (defaults to 7 days ago)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @Parameter(description = "Maximum number of videos to return")
            @RequestParam(defaultValue = "10") int limit) {

        // Default to 7 days ago if not specified
        LocalDateTime startDate = since != null ? since : LocalDateTime.now().minusDays(7);

        log.debug("Getting most liked videos since {} with limit {}", startDate, limit);

        List<Long> trendingVideos = videoLikeService.getMostLikedVideos(startDate, limit);

        return ResponseEntity.ok(trendingVideos);
    }

    /**
     * Get user's liked videos
     *
     * Returns a paginated list of videos that the specified user has liked.
     * Users can only access their own likes unless they are admins.
     */
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user's liked videos",
            description = "Returns a paginated list of videos that the specified user has liked. " +
                    "Users can only access their own likes unless they are admins."
    )
    @ApiResponse(responseCode = "200", description = "User's likes retrieved")
    @ApiResponse(responseCode = "403", description = "Not authorized to view this user's likes")
    @PreAuthorize("@likeSecurityService.canAccessUserLikes(#userId, principal)")
    public ResponseEntity<Page<VideoLikeResponse>> getUserLikes(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting likes for user with ID: {}", userId);

        // Create pageable
        PageRequest pageRequest = PageRequest.of(page, size);

        // Get user likes
        Page<VideoLike> likes = videoLikeService.getUserLikes(userId, pageRequest);

        // Map to response DTOs
        Page<VideoLikeResponse> response = likes.map(this::convertToVideoLikeResponse);

        return ResponseEntity.ok(response);
    }

}