/**
 * Session Security Service
 * Location: src/main/java/com/videoanalytics/security/SessionSecurityService.java
 *
 * This service provides security checks for view session operations, ensuring
 * that users can only access and modify their own viewing sessions unless
 * they have admin privileges.
 */
package com.videoanalytics.security;

import com.videoanalytics.video.model.ViewSession;
import com.videoanalytics.video.repository.ViewSessionRepository;
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
public class SessionSecurityService {

    private final ViewSessionRepository viewSessionRepository;

    /**
     * Checks if the current user is the owner of the session.
     * This is used to determine if the user can update or end a specific session.
     *
     * @param sessionId ID of the view session to check
     * @param principal Authentication principal representing the current user
     * @return true if the user is the session owner, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isSessionOwner(Long sessionId, Object principal) {
        if (principal == null) {
            return false;
        }

        Long userId = extractUserId(principal);
        return viewSessionRepository.findById(sessionId)
                .map(session -> session.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * Checks if the current user is authorized to access a user's viewing history.
     * Users can access their own history, and admins can access any user's history.
     *
     * @param userId ID of the user whose history is being accessed
     * @param principal Authentication principal representing the current user
     * @return true if access is allowed, false otherwise
     */
    public boolean canAccessUserHistory(Long userId, Object principal) {
        if (principal == null) {
            return false;
        }

        // Check if user has admin role
        if (hasAdminRole(principal)) {
            return true;
        }

        // Check if user is accessing their own history
        return userId.equals(extractUserId(principal));
    }
    
}