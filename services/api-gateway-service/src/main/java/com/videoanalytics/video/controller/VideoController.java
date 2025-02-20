/**
 * Video Controller
 * Location: src/main/java/com/videoanalytics/video/controller/VideoController.java
 *
 * This controller provides the REST API endpoints for managing videos, including
 * uploading, retrieving, updating, and deleting videos, as well as searching and filtering.
 */
package com.videoanalytics.video.controller;



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

}