/**
 * User Principal
 * Location: src/main/java/com/videoanalytics/security/jwt/UserPrincipal.java
 *
 * This class represents the authenticated user in the security context.
 * It implements UserDetails interface for Spring Security integration.
 */
package com.videoanalytics.security.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.videoanalytics.auth.model.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Creates a UserPrincipal from a User entity.
     */
    public static UserPrincipal create(User user) {
        // Convert user roles to Spring Security authorities
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

    @Override