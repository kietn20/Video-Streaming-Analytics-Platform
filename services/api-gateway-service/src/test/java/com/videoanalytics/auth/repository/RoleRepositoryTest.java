/**
 * Role Repository Integration Tests
 * Location: src/test/java/com/videoanalytics/auth/repository/RoleRepositoryTest.java
 */
package com.videoanalytics.auth.repository;

import com.videoanalytics.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void whenSaveRole_thenRoleIsPersisted() {
        // Given
        Role role = new Role("ROLE_USER", "Basic user role");

        // When
        Role savedRole = roleRepository.save(role);

        // Then
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("ROLE_USER");
        assertThat(savedRole.getDescription()).isEqualTo("Basic user role");
    }

    @Test
    void whenFindByName_thenReturnRole() {
        // Given
        Role role = new Role("ROLE_ADMIN", "Admin role");
        roleRepository.save(role);

        // When
        Optional<Role> found = roleRepository.findByName("ROLE_ADMIN");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void whenFindByNameIn_thenReturnRoles() {
        // Given
        Role userRole = new Role("ROLE_USER", "Basic user role");
        Role adminRole = new Role("ROLE_ADMIN", "Admin role");
        roleRepository.save(userRole);
        roleRepository.save(adminRole);

        Set<String> roleNames = new HashSet<>();
        roleNames.add("ROLE_USER");
        roleNames.add("ROLE_ADMIN");

        // When
        Set<Role> found = roleRepository.findByNameIn(roleNames);

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Role::getName).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }
}
