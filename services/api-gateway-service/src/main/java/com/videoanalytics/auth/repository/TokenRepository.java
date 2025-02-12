/**
 * Repository interface for Token entity
 * Location: src/main/java/com/videoanalytics/auth/repository/TokenRepository.java
 */
package com.videoanalytics.auth.repository;

import com.videoanalytics.auth.model.Token;
import com.videoanalytics.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    List<Token> findByUserAndRevoked(User user, boolean revoked);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiryDate < ?1")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("UPDATE Token t SET t.revoked = true WHERE t.user = ?1")
    void revokeAllUserTokens(User user);
}