/**
 * Web Security Configuration Tests
 * Location: src/test/java/com/videoanalytics/auth/config/WebSecurityConfigTest.java
 */
package com.videoanalytics.auth.config;

import com.videoanalytics.auth.security.AuthenticationManager;
import com.videoanalytics.auth.security.SecurityContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import(WebSecurityConfig.class)
class WebSecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private SecurityContextRepository securityContextRepository;

    @BeforeEach
    void setUp() {
        // Configure security mocks
        when(securityContextRepository.load(any()))
                .thenReturn(Mono.empty());
    }

    @Test
    void whenAccessingPublicEndpoint_thenAllowed() {
        webTestClient
                .get()
                .uri("/api/auth/login")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenAccessingProtectedEndpointWithoutAuth_thenUnauthorized() {
        webTestClient
                .get()
                .uri("/api/protected")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenOptionsRequest_thenAllowed() {
        webTestClient
                .options()
                .uri("/api/protected")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenAccessingActuatorHealth_thenAllowed() {
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}