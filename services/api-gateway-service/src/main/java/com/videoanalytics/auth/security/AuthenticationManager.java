/**
 * Custom Authentication Manager
 * Location: src/main/java/com/videoanalytics/auth/security/AuthenticationManager.java
 */
package com.videoanalytics.auth.security;

import com.videoanalytics.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        return Mono.justOrEmpty(authToken)
                .filter(token -> authService.validateToken(token).block())
                .map(token -> {
                    String username = tokenProvider.getUsernameFromToken(token);
                    List<String> roles = tokenProvider.getRolesFromToken(token);

                    return new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList())
                    );
                });
    }
}