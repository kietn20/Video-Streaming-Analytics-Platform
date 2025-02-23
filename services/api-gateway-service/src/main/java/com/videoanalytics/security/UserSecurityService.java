/**
 * User Security Service
 * Location: src/main/java/com/videoanalytics/security/UserSecurityService.java
 *
 * This service provides security checks for user-related operations, ensuring
 * that users can only access their own data unless they have admin privileges.
 */
package com.videoanalytics.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSecurityService {

    /**
     * Checks if the current user is the same as the requested user.
     * This is used to determine if the user can access their own analytics
     * and engagement data.
     *
     * @param userId ID of the user whose data is being accessed
     * @param principal Authentication principal representing the current user
     * @return true if the current user is the same as the requested user or is an admin, false otherwise
     */
    public boolean isSameUser(Long userId, Object principal) {
        if (principal == null) {
            return false;
        }

        // Check if user has admin role
        if (hasAdminRole(principal)) {
            return true;
        }

        // Check if user is accessing their own data
        return userId.equals(extractUserId(principal));
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