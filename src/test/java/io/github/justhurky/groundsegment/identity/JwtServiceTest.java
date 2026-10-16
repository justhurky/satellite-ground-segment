package io.github.justhurky.groundsegment.identity;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:26:53Z");
    private static final String SECRET = "integration-test-signing-key-0123456789abcdef";

    private final JwtService jwtService = serviceAt(NOW, SECRET, "satellite-ground-segment");

    @Test
    void issuesATokenThatParsesBackToTheSameSubjectAndRoles() {
        JwtService.IssuedToken token = jwtService.issue("operator", EnumSet.of(Role.OPERATOR, Role.ANALYST));

        JwtService.TokenPrincipal principal = jwtService.parse(token.token()).orElseThrow();

        assertThat(principal.username()).isEqualTo("operator");
        assertThat(principal.roles()).containsExactlyInAnyOrder(Role.OPERATOR, Role.ANALYST);
    }

    @Test
    void reportsTheExpiryDerivedFromTheConfiguredTimeToLive() {
        JwtService.IssuedToken token = jwtService.issue("operator", EnumSet.of(Role.OPERATOR));

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(token.ttl()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        JwtService other = serviceAt(NOW, "another-signing-key-0123456789abcdefghijk", "satellite-ground-segment");
        JwtService.IssuedToken forged = other.issue("operator", EnumSet.of(Role.ADMIN));

        assertThat(jwtService.parse(forged.token())).isEmpty();
    }

    @Test
    void rejectsATokenFromAnotherIssuer() {
        JwtService other = serviceAt(NOW, SECRET, "someone-else");
        JwtService.IssuedToken foreign = other.issue("operator", EnumSet.of(Role.OPERATOR));

        assertThat(jwtService.parse(foreign.token())).isEmpty();
    }

    @Test
    void rejectsATokenWhoseLifetimeHasElapsed() {
        JwtService.IssuedToken token = jwtService.issue("operator", EnumSet.of(Role.OPERATOR));
        JwtService later = serviceAt(NOW.plus(Duration.ofHours(1)), SECRET, "satellite-ground-segment");

        assertThat(later.parse(token.token())).isEmpty();
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.issue("operator", EnumSet.of(Role.OPERATOR)).token();
        String tampered = token.substring(0, token.length() - 2) + "xy";

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void rejectsAValueThatIsNotAToken() {
        assertThat(jwtService.parse("not-a-token")).isEmpty();
    }

    private JwtService serviceAt(Instant instant, String secret, String issuer) {
        GroundSegmentProperties properties = new GroundSegmentProperties(
                new GroundSegmentProperties.Jwt(secret, Duration.ofMinutes(15), issuer),
                new GroundSegmentProperties.Crypto("5wwTGlnv/8J0FwEGgViotGHCMy1/uLscwsQQcT5CmEA="),
                new GroundSegmentProperties.Telemetry(64, 4096, 2047),
                new GroundSegmentProperties.Uplink(20, Duration.ofMinutes(5)));
        return new JwtService(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }
}
