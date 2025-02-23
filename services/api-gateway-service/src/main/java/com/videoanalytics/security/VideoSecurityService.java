/**
 * Video Security Service
 * Location: src/main/java/com/videoanalytics/security/VideoSecurityService.java
 *
 * This service provides security checks for video-related operations, ensuring
 * that users can only access and modify videos they own unless they have admin privileges.
 */
package com.videoanalytics.security;

import com.videoanalytics.video.model.Video;
import com.videoanalytics.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoSecurityService {

    private final VideoRepository videoRepository;

    /**
     * Checks if the current user is the owner of the video or has admin privileges.
     * This is used to determine if the user can update, delete, or access detailed analytics
     * for a specific video.
     *
     * @param videoId ID of the video to check
     * @param principal Authentication principal representing the current user
     * @return true if the user is the owner or an admin, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isVideoOwnerOrAdmin(Long videoId, Object principal) {
        if (principal == null) {
            return false;
        }

        // Check if user has admin role
        if (hasAdminRole(principal)) {
            return true;
        }

        // Check if user is the video owner
        return isVideoOwner(videoId, principal);
    }

    /**
     * Checks if the current user is the owner of the video.
     * This is used for operations where even admins should only see their own data
     * (like personal dashboards).
     *
     * @param videoId ID of the video to check
     * @param principal Authentication principal representing the current user
     * @return true if the user is the owner, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isVideoOwner(Long videoId, Object principal) {
        if (principal == null) {
            return false;
        }

        Long userId = extractUserId(principal);
        return videoRepository.findById(videoId)
                .map(video -> video.getUploadedBy().equals(userId))
                .orElse(false);
    }

    /**
     * Extracts user ID from the authentication principal.
     *
     * @param principal Authentication principal
     * @return User ID as a Long
     */
    private Long extractUserId(Object principal) {
        if (principal instanceof UserDetails) {
            // In this example, we assume username is the user ID
            return Long.parseLong(((UserDetails) principal).getUsername());
        } else if (principal instanceof Authentication) {
            return Long.parseLong(((Authentication) principal).getName());
        }
        throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass());
    }

    /**
     * Checks if the user has the ADMIN role.
     *
     * @param principal Authentication principal
     * @return true if the user has admin role, false otherwise
     */
    private boolean hasAdminRole(Object principal) {
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if (principal instanceof Authentication) {
            return ((Authentication) principal).getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return false;
    }
}