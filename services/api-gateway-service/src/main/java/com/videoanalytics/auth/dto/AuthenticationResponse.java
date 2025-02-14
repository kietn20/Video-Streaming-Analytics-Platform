/**
 * Authentication Response DTO
 * Location: src/main/java/com/videoanalytics/auth/dto/AuthenticationResponse.java
 */
package com.videoanalytics.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private Set<String> roles;
}