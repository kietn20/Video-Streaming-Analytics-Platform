/**
 * Token Repository Integration Tests
 * Location: src/test/java/com/videoanalytics/auth/repository/TokenRepositoryTest.java
 */
package com.videoanalytics.auth.repository;

import com.videoanalytics.auth.model.Token;
import com.videoanalytics.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TokenRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User("tokenuser", "token@example.com", "password123"));
    }

    @Test
    void whenSaveToken_thenTokenIsPersisted() {
        // Given
        Token token = new Token(
                "refresh-token-123",
                testUser,
                LocalDateTime.now().plusDays(7)
        );

        // When
        Token savedToken = tokenRepository.save(token);

        // Then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo("refresh-token-123");
        assertThat(savedToken.getUser()).isEqualTo(testUser);
    }

    @Test
    void whenFindByToken_thenReturnToken() {
        // Given
        Token token = new Token(
                "unique-token-456",
                testUser,
                LocalDateTime.now().plusDays(7)
        );
        tokenRepository.save(token);

        // When
        Optional<Token> found = tokenRepository.findByToken("unique-token-456");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isEqualTo(testUser);
    }

    @Test
    void whenFindByUserAndRevoked_thenReturnTokens() {
        // Given
        Token token1 = new Token("token-1", testUser, LocalDateTime.now().plusDays(7));
        Token token2 = new Token("token-2", testUser, LocalDateTime.now().plusDays(7));
        token2.setRevoked(true);
        tokenRepository.save(token1);
        tokenRepository.save(token2);

        // When
        List<Token> activeTokens = tokenRepository.findByUserAndRevoked(testUser, false);
        List<Token> revokedTokens = tokenRepository.findByUserAndRevoked(testUser, true);

        // Then
        assertThat(activeTokens).hasSize(1);
        assertThat(revokedTokens).hasSize(1);
        assertThat(activeTokens.get(0).getToken()).isEqualTo("token-1");
        assertThat(revokedTokens.get(0).getToken()).isEqualTo("token-2");
    }

    @Test
    void whenDeleteExpiredTokens_thenOnlyExpiredTokensAreRemoved() {
        // Given
        Token expiredToken = new Token("expired-token", testUser, LocalDateTime.now().minusDays(1));
        Token validToken = new Token("valid-token", testUser, LocalDateTime.now().plusDays(7));
        tokenRepository.save(expiredToken);
        tokenRepository.save(validToken);

        // When
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Then
        List<Token> remainingTokens = tokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo("valid-token");
    }

    @Test
    void whenRevokeAllUserTokens_thenAllTokensAreRevoked() {
        // Given
        Token token1 = new Token("token-1", testUser, LocalDateTime.now().plusDays(7));
        Token token2 = new Token("token-2", testUser, LocalDateTime.now().plusDays(7));
        tokenRepository.save(token1);
        tokenRepository.save(token2);

        // When
        tokenRepository.revokeAllUserTokens(testUser);

        // Then
        List<Token> revokedTokens = tokenRepository.findByUserAndRevoked(testUser, true);
        assertThat(revokedTokens).hasSize(2);
    }
}