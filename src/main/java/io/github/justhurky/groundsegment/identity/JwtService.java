package io.github.justhurky.groundsegment.identity;

import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ROLES_CLAIM = "roles";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final String issuer;
    private final Clock clock;

    public JwtService(GroundSegmentProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
        this.issuer = properties.jwt().issuer();
        this.clock = clock;
    }

    public IssuedToken issue(String username, Set<Role> roles) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(ROLES_CLAIM, roles.stream().map(Role::name).toList())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt, accessTokenTtl);
    }

    public Optional<TokenPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new TokenPrincipal(claims.getSubject(), readRoles(claims)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Set<Role> readRoles(Claims claims) {
        List<?> claimed = claims.get(ROLES_CLAIM, List.class);
        if (claimed == null) {
            return EnumSet.noneOf(Role.class);
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (Object value : claimed) {
            roles.add(Role.valueOf(String.valueOf(value)));
        }
        return roles;
    }

    public record IssuedToken(String token, Instant expiresAt, Duration ttl) {
    }

    public record TokenPrincipal(String username, Set<Role> roles) {
    }
}
