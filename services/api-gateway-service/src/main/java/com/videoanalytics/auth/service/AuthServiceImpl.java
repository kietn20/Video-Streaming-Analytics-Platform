/**
 * Auth Service Implementation
 * Location: src/main/java/com/videoanalytics/auth/service/AuthServiceImpl.java
 */
package com.videoanalytics.auth.service;

import com.videoanalytics.auth.dto.AuthenticationRequest;
import com.videoanalytics.auth.dto.AuthenticationResponse;
import com.videoanalytics.auth.dto.RegistrationRequest;
import com.videoanalytics.auth.model.Role;
import com.videoanalytics.auth.model.Token;
import com.videoanalytics.auth.model.User;
import com.videoanalytics.auth.repository.RoleRepository;
import com.videoanalytics.auth.repository.TokenRepository;
import com.videoanalytics.auth.repository.UserRepository;
import com.videoanalytics.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveAuthenticationManager authenticationManager;

    @Override
    @Transactional
    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()))
                .flatMap(auth -> {
                    String username = auth.getName();
                    return userRepository.findByUsername(username)
                            .map(user -> {
                                String accessToken = jwtTokenProvider.generateAccessToken(user);
                                String refreshToken = generateRefreshToken(user);
                                return createAuthResponse(user, accessToken, refreshToken);
                            });
                });
    }

    @Override
    @Transactional
    public Mono<AuthenticationResponse> register(RegistrationRequest request) {
        return Mono.just(request)
                .filter(req -> !userRepository.existsByUsername(req.getUsername()))
                .filter(req -> !userRepository.existsByEmail(req.getEmail()))
                .map(req -> {
                    User user = new User();
                    user.setUsername(req.getUsername());
                    user.setEmail(req.getEmail());
                    user.setPassword(passwordEncoder.encode(req.getPassword()));

                    Set<String> strRoles = req.getRoles();
                    Set<Role> roles = new HashSet<>();

                    if (strRoles == null || strRoles.isEmpty()) {
                        Role userRole = roleRepository.findByName("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Default role not found"));
                        roles.add(userRole);
                    } else {
                        roles = roleRepository.findByNameIn(strRoles);
                        if (roles.isEmpty()) {
                            throw new RuntimeException("Role not found");
                        }
                    }

                    user.setRoles(roles);
                    return userRepository.save(user);
                })
                .map(user -> {
                    String accessToken = jwtTokenProvider.generateAccessToken(user);
                    String refreshToken = generateRefreshToken(user);
                    return createAuthResponse(user, accessToken, refreshToken);
                });
    }

    @Override
    @Transactional
    public Mono<AuthenticationResponse> refreshToken(String refreshToken) {
        return Mono.just(refreshToken)
                .flatMap(token -> tokenRepository.findByToken(token))
                .filter(Token::isValid)
                .map(Token::getUser)
                .map(user -> {
                    String accessToken = jwtTokenProvider.generateAccessToken(user);
                    String newRefreshToken = generateRefreshToken(user);
                    return createAuthResponse(user, accessToken, newRefreshToken);
                });
    }

    @Override
    @Transactional
    public Mono<Void> logout(String refreshToken) {
        return Mono.just(refreshToken)
                .flatMap(token -> tokenRepository.findByToken(token))
                .map(token -> {
                    token.setRevoked(true);
                    return tokenRepository.save(token);
                })
                .then();
    }

    @Override
    public Mono<Boolean> validateToken(String token) {
        return Mono.just(jwtTokenProvider.validateToken(token));
    }

    private String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        Token refreshToken = new Token(
                token,
                user,
                LocalDateTime.now().plusDays(7)
        );
        tokenRepository.save(refreshToken);
        return token;
    }

    private AuthenticationResponse createAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}