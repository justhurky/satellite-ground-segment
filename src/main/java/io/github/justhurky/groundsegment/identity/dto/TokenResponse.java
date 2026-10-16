package io.github.justhurky.groundsegment.identity.dto;

import io.github.justhurky.groundsegment.identity.JwtService;
import io.github.justhurky.groundsegment.identity.Role;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Instant expiresAt,
        List<String> roles) {

    public static TokenResponse of(JwtService.IssuedToken token, Set<Role> roles) {
        return new TokenResponse(
                token.token(),
                "Bearer",
                token.ttl().toSeconds(),
                token.expiresAt(),
                roles.stream().map(Role::name).sorted().toList());
    }
}
