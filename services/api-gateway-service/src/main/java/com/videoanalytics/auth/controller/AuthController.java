/**
 * Authentication Controller
 * Location: src/main/java/com/videoanalytics/auth/controller/AuthController.java
 *
 * This controller handles user authentication, including login, registration,
 * and token refresh operations.
 */
package com.videoanalytics.auth.controller;

import com.videoanalytics.auth.dto.AuthenticationRequest;
import com.videoanalytics.auth.dto.AuthenticationResponse;
import com.videoanalytics.auth.dto.RegistrationRequest;
import com.videoanalytics.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operations for user authentication and registration")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * User login
     *
     * Authenticates a user with username and password, returning a JWT token.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates a user with username and password")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Processing login request for user: {}", request.getUsername());
        return ResponseEntity.ok(authService.authenticate(request));
    }

    /**
     * User registration
     *
     * Creates a new user account and returns a JWT token.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        log.info("Processing registration request for user: {}", request.getUsername());
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Token refresh
     *
     * Issues a new access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Issues a new access token using a valid refresh token")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestParam String refreshToken) {
        log.info("Processing token refresh request");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    /**
     * User logout
     *
     * Invalidates the refresh token, effectively logging the user out.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the refresh token")
    public ResponseEntity<Void> logout(@RequestParam String refreshToken) {
        log.info("Processing logout request");
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}