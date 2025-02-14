/**
 * Auth Service Interface
 * Location: src/main/java/com/videoanalytics/auth/service/AuthService.java
 */
package com.videoanalytics.auth.service;

import com.videoanalytics.auth.dto.AuthenticationRequest;
import com.videoanalytics.auth.dto.AuthenticationResponse;
import com.videoanalytics.auth.dto.RegistrationRequest;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<AuthenticationResponse> authenticate(AuthenticationRequest request);
    Mono<AuthenticationResponse> register(RegistrationRequest request);
    Mono<AuthenticationResponse> refreshToken(String refreshToken);
    Mono<Void> logout(String refreshToken);
    Mono<Boolean> validateToken(String token);
}