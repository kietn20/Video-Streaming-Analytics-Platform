/**
 * Repository interface for Role entity
 * Location: src/main/java/com/videoanalytics/auth/repository/RoleRepository.java
 */
package com.videoanalytics.auth.repository;

import com.videoanalytics.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Set<Role> findByNameIn(Set<String> names);
}