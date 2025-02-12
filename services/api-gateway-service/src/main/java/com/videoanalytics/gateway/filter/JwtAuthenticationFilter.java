/**
 * JWT Authentication Filter Implementation
 * Location: src/main/java/com/videoanalytics/gateway/filter/JwtAuthenticationFilter.java
 */
package com.videoanalytics.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${gateway.jwt.secret}")
    private String jwtSecret;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Check if Authorization header exists
            List<String> authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.isEmpty()) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header"));
            }

            String token = authHeader.get(0);

            // Validate Bearer token format
            if (!token.startsWith(BEARER_PREFIX)) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format"));
            }

            // Extract token without Bearer prefix
            token = token.substring(BEARER_PREFIX.length());

            try {
                // Parse and validate token
                Claims claims = validateToken(token);

                // Add user information to headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(USER_ID_HEADER, claims.getSubject())
                        .header(USER_ROLES_HEADER, claims.get("roles", String.class))
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
            }
        };
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static class Config {
        // Configuration properties if needed
        private String roleHeader;

        public String getRoleHeader() {
            return roleHeader;
        }

        public void setRoleHeader(String roleHeader) {
            this.roleHeader = roleHeader;
        }
    }
}