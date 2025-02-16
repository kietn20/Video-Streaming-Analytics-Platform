/**
 * Security Context Repository
 * Location: src/main/java/com/videoanalytics/auth/security/SecurityContextRepository.java
 */
package com.videoanalytics.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final AuthenticationManager authenticationManager;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty(); // Stateless, no need to save context
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String authToken = authHeader.substring(BEARER_PREFIX.length());
            Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);

            return authenticationManager.authenticate(auth)
                    .map(SecurityContextImpl::new);
        }

        return Mono.empty();
    }
}