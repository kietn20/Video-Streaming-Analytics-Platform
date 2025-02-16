/**
 * CORS Configuration Tests
 * Location: src/test/java/com/videoanalytics/auth/config/CorsConfigTest.java
 */
package com.videoanalytics.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import(CorsConfig.class)
class CorsConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenCorsRequest_thenAllowsConfiguredOrigin() {
        webTestClient
                .options()
                .uri("/api/test")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                .expectStatus().isOk();
    }

    @Test
    void whenInvalidOrigin_thenRejects() {
        webTestClient
                .options()
                .uri("/api/test")
                .header("Origin", "http://unauthorized-origin.com")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin")
                .expectStatus().isForbidden();
    }
}