/**
 * Authentication Controller Test
 * Location: src/test/java/com/videoanalytics/auth/controller/AuthControllerTest.java
 */
package com.videoanalytics.auth.controller;

import com.videoanalytics.auth.dto.AuthenticationRequest;
import com.videoanalytics.auth.dto.AuthenticationResponse;
import com.videoanalytics.auth.dto.RegistrationRequest;
import com.videoanalytics.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthService authService;

    @Test
    void whenLogin_withValidCredentials_thenSuccess() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        AuthenticationResponse response = createMockAuthResponse();
        when(authService.authenticate(any())).thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", "refreshToken=.*")
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("mock-access-token")
                .jsonPath("$.username").isEqualTo("testuser");
    }

    @Test
    void whenLogin_withInvalidCredentials_thenUnauthorized() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authService.authenticate(any())).thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenRegister_withValidData_thenSuccess() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        AuthenticationResponse response = createMockAuthResponse();
        when(authService.register(any())).thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", "refreshToken=.*")
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("mock-access-token")
                .jsonPath("$.username").isEqualTo("testuser");
    }

    @Test
    void whenRefreshToken_withValidToken_thenSuccess() {
        // Given
        AuthenticationResponse response = createMockAuthResponse();
        when(authService.refreshToken(anyString())).thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/api/auth/refresh")
                .cookie("refreshToken", "valid-refresh-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", "refreshToken=.*")
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("mock-access-token");
    }

    @Test
    void whenLogout_thenSuccess() {
        // Given
        when(authService.logout(anyString())).thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/api/auth/logout")
                .cookie("refreshToken", "valid-refresh-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", "refreshToken=.*;.*Max-Age=0.*");
    }

    private AuthenticationResponse createMockAuthResponse() {
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");

        return AuthenticationResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .username("testuser")
                .roles(roles)
                .build();
    }
}