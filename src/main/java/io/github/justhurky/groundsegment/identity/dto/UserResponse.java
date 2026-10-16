package io.github.justhurky.groundsegment.identity.dto;

import io.github.justhurky.groundsegment.identity.AppUser;
import io.github.justhurky.groundsegment.identity.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String username, List<String> roles, boolean enabled, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream().map(Role::name).sorted().toList(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
