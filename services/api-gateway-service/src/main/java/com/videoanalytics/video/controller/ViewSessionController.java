/**
 * View Session Controller
 * Location: src/main/java/com/videoanalytics/video/controller/ViewSessionController.java
 *
 * This controller provides REST API endpoints for managing video viewing sessions,
 * including starting, updating, and ending sessions, as well as retrieving session
 * analytics data.
 */
package com.videoanalytics.video.controller;

import com.videoanalytics.video.dto.ViewSessionRequest;
import com.videoanalytics.video.dto.ViewSessionResponse;
import com.videoanalytics.video.model.ViewSession;
import com.videoanalytics.video.service.ViewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "View Sessions", description = "Operations for tracking and analyzing video viewing sessions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class ViewSessionController {

    private final ViewSessionService viewSessionService;

    /**
     * Start a new viewing session
     *
     * Creates a new session when a user starts watching a video.
     * Captures initial metadata like device type, platform, and IP address.
     */
    @PostMapping
    @Operation(
            summary = "Start a new viewing session",
            description = "Creates a new session when a user starts watching a video. " +
                    "Records the viewer's device information and starting timestamp."
    )
    @ApiResponse(responseCode = "201", description = "Session successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ViewSessionResponse> startSession(
            @Valid @RequestBody ViewSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Starting new view session for video ID: {}", request.getVideoId());

        // Set the authenticated user ID
        Long userId = extractUserId(userDetails);
        request.setUserId(userId);

        // Create the session
        ViewSession session = viewSessionService.startSession(request);

        // Convert to response DTO
        ViewSessionResponse response = convertToViewSessionResponse(session);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Update an ongoing viewing session
     *
     * Updates an existing session with new metrics like current position,
     * bitrate changes, quality switches, or buffer events.
     */
    @PutMapping("/{sessionId}")
    @Operation(
            summary = "Update a viewing session",
            description = "Updates an existing session with new metrics like current position, " +
                    "bitrate changes, quality switches, or buffer events."
    )
    @ApiResponse(responseCode = "200", description = "Session successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Not authorized to update this session")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @PreAuthorize("@sessionSecurityService.isSessionOwner(#sessionId, principal)")
    public ResponseEntity<ViewSessionResponse> updateSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody ViewSessionRequest request) {

        log.info("Updating view session with ID: {}", sessionId);

        ViewSession updatedSession = viewSessionService.updateSession(sessionId, request);
        ViewSessionResponse response = convertToViewSessionResponse(updatedSession);

        return ResponseEntity.ok(response);
    }

    /**
     * End a viewing session
     *
     * Marks a session as completed and records the final watch duration.
     */
    @PostMapping("/{sessionId}/end")
    @Operation(
            summary = "End a viewing session",
            description = "Marks a session as completed and records the final watch duration"
    )
    @ApiResponse(responseCode = "204", description = "Session successfully ended")
    @ApiResponse(responseCode = "403", description = "Not authorized to end this session")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @PreAuthorize("@sessionSecurityService.isSessionOwner(#sessionId, principal)")
    public ResponseEntity<Void> endSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {

        log.info("Ending view session with ID: {}", sessionId);

        viewSessionService.endSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user's viewing history
     *
     * Retrieves a paginated list of a user's viewing sessions.
     * Users can only access their own viewing history.
     */
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user's viewing history",
            description = "Retrieves a paginated list of a user's viewing sessions. " +
                    "Users can only access their own viewing history unless they are admins."
    )
    @ApiResponse(responseCode = "200", description = "User's viewing history")
    @ApiResponse(responseCode = "403", description = "Not authorized to view this user's history")
    @PreAuthorize("@sessionSecurityService.canAccessUserHistory(#userId, principal)")
    public ResponseEntity<Page<ViewSessionResponse>> getUserSessions(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching viewing history for user ID: {}", userId);

        // Create pageable
        PageRequest pageRequest = PageRequest.of(page, size);

        // Get user sessions
        Page<ViewSession> sessions = viewSessionService.getUserSessions(userId, pageRequest);

        // Map to response DTOs
        Page<ViewSessionResponse> response = sessions.map(this::convertToViewSessionResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get device distribution analytics
     *
     * Retrieves analytics on device types used to watch a specific video.
     */
    @GetMapping("/analytics/devices/{videoId}")
    @Operation(
            summary = "Get device distribution analytics",
            description = "Retrieves analytics on device types used to watch a specific video"
    )
    @ApiResponse(responseCode = "200", description = "Device distribution data")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<Map<String, Long>> getDeviceDistribution(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.debug("Getting device distribution for video ID: {}", videoId);

        Map<String, Long> distribution = viewSessionService.getDeviceDistribution(videoId);
        return ResponseEntity.ok(distribution);
    }

    /**
     * Get average watch duration
     *
     * Retrieves the average time users spend watching a specific video.
     */
    @GetMapping("/analytics/duration/{videoId}")
    @Operation(
            summary = "Get average watch duration",
            description = "Retrieves the average time users spend watching a specific video"
    )
    @ApiResponse(responseCode = "200", description = "Average watch duration in seconds")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<Double> getAverageWatchDuration(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.debug("Getting average watch duration for video ID: {}", videoId);

        double averageDuration = viewSessionService.getAverageWatchDuration(videoId);
        return ResponseEntity.ok(averageDuration);
    }

    /**
     * Get video completion rate
     *
     * Retrieves the percentage of views where users watched at least 90% of the video.
     */
    @GetMapping("/analytics/completion/{videoId}")
    @Operation(
            summary = "Get video completion rate",
            description = "Retrieves the number of views where users watched at least 90% of the video"
    )
    @ApiResponse(responseCode = "200", description = "Completion count")
    @ApiResponse(responseCode = "404", description = "Video not found")
    @PreAuthorize("hasRole('ADMIN') or @videoSecurityService.isVideoOwner(#videoId, principal)")
    public ResponseEntity<Long> getCompletionRate(
            @Parameter(description = "Video ID", required = true)
            @PathVariable Long videoId) {

        log.debug("Getting completion rate for video ID: {}", videoId);

        long completionCount = viewSessionService.getCompletionRate(videoId);
        return ResponseEntity.ok(completionCount);
    }

    /**
     * Get sessions in time range
     *
     * Admin endpoint to retrieve all viewing sessions within a specific time period.
     */
    @GetMapping("/analytics/timerange")
    @Operation(
            summary = "Get sessions in time range",
            description = "Admin endpoint to retrieve all viewing sessions within a specific time period"
    )
    @ApiResponse(responseCode = "200", description = "List of sessions")
    @ApiResponse(responseCode = "403", description = "Not authorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ViewSessionResponse>> getSessionsInTimeRange(
            @Parameter(description = "Start time", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.debug("Fetching sessions between {} and {}", start, end);

        List<ViewSession> sessions = viewSessionService.getSessionsInTimeRange(start, end);

        // Map to response DTOs
        List<ViewSessionResponse> response = sessions.stream()
                .map(this::convertToViewSessionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
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
     * Converts a ViewSession entity to a ViewSessionResponse DTO
     */
    private ViewSessionResponse convertToViewSessionResponse(ViewSession session) {
        return ViewSessionResponse.builder()
                .id(session.getId())
                .videoId(session.getVideo().getId())
                .userId(session.getUserId())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .watchDuration(session.getWatchDuration())
                .lastPosition(session.getLastPosition())
                .deviceType(session.getDeviceType())
                .platform(session.getPlatform())
                .qualitySwitches(session.getQualitySwitches())
                .bufferEvents(session.getBufferEvents())
                .averageBitrate(session.getAverageBitrate())
                .build();
    }

}