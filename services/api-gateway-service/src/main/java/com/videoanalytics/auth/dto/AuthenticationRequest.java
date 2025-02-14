/**
 * Authentication Request DTO
 * Location: src/main/java/com/videoanalytics/auth/dto/AuthenticationRequest.java
 */
package com.videoanalytics.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthenticationRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}