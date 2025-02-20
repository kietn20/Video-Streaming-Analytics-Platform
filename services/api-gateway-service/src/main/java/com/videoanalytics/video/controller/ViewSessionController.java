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


}