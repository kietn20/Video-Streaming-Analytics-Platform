/**
 * User Repository Integration Tests
 * Location: src/test/java/com/videoanalytics/auth/repository/UserRepositoryTest.java
 */
package com.videoanalytics.auth.repository;

import com.videoanalytics.auth.model.Role;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = roleRepository.save(new Role("ROLE_USER", "Basic user role"));
    }

    @Test
    void whenSaveUser_thenUserIsPersisted() {
        // Given
        User user = new User("testuser", "test@example.com", "password123");
        user.addRole(userRole);

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getRoles()).contains(userRole);
    }

    @Test
    void whenFindByUsername_thenReturnUser() {
        // Given
        User user = new User("johnsmith", "john@example.com", "password123");
        user.addRole(userRole);
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByUsername("johnsmith");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        // Given
        User user = new User("jane", "jane@example.com", "password123");
        user.addRole(userRole);
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("jane@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jane");
    }

    @Test
    void whenExistsByUsername_thenReturnTrue() {
        // Given
        User user = new User("existinguser", "existing@example.com", "password123");
        user.addRole(userRole);
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByUsername("existinguser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByEmail_thenReturnTrue() {
        // Given
        User user = new User("emailuser", "exists@example.com", "password123");
        user.addRole(userRole);
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("exists@example.com");

        // Then
        assertThat(exists).isTrue();
    }
}