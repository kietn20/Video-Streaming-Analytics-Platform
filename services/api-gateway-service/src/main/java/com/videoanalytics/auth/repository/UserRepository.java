/**
 * Repository interface for User entity
 * Location: src/main/java/com/videoanalytics/auth/repository/UserRepository.java
 */
package com.videoanalytics.auth.repository;

import com.videoanalytics.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}