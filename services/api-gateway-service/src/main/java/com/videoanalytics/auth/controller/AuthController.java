/**
 * Authentication Controller
 * Location: src/main/java/com/videoanalytics/auth/controller/AuthController.java
 */
package com.videoanalytics.auth.controller;

import com.videoanalytics.auth.dto.AuthenticationRequest;
import com.videoanalytics.auth.dto.AuthenticationResponse;
import com.videoanalytics.auth.dto.RegistrationRequest;
import com.videoanalytics.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns JWT tokens
     * @param request Contains username and password
     * @return JWT access and refresh tokens
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthenticationResponse>> login(
            @Valid @RequestBody AuthenticationRequest request) {

        return authService.authenticate(request)
                .map(this::createSuccessResponse);
    }

    /**
     * Registers a new user
     * @param request User registration details
     * @return JWT tokens for the new user
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthenticationResponse>> register(
            @Valid @RequestBody RegistrationRequest request) {

        return authService.register(request)
                .map(this::createSuccessResponse);
    }

    /**
     * Refreshes an access token using a refresh token
     * @param refreshToken The refresh token
     * @return New JWT tokens
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthenticationResponse>> refresh(
            @CookieValue(name = "refreshToken") String refreshToken) {

        return authService.refreshToken(refreshToken)
                .map(this::createSuccessResponse);
    }

    /**
     * Logs out a user by invalidating their refresh token
     * @param refreshToken The refresh token to invalidate
     * @return Empty response with cleared cookies
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @CookieValue(name = "refreshToken") String refreshToken) {

        return authService.logout(refreshToken)
                .then(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, createClearedRefreshTokenCookie().toString())
                        .build()));
    }

    /**
     * Creates a response with authentication tokens
     * Sets refresh token as an HTTP-only cookie and returns access token in body
     */
    private ResponseEntity<AuthenticationResponse> createSuccessResponse(AuthenticationResponse response) {
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

    /**
     * Creates a cookie that clears the refresh token
     */
    private ResponseCookie createClearedRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}