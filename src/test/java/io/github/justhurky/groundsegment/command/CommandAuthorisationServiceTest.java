package io.github.justhurky.groundsegment.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.justhurky.groundsegment.command.dto.SubmitCommandRequest;
import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import io.github.justhurky.groundsegment.identity.CurrentActor;
import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CommandAuthorisationServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:26:53Z");

    private final CommandCatalogue catalogue = new CommandCatalogue(Map.of(
            "ping", new CommandCatalogue.Rule(CommandCriticality.ROUTINE,
                    EnumSet.of(MissionPhase.COMMISSIONING, MissionPhase.NOMINAL, MissionPhase.SAFE_MODE),
                    Role.OPERATOR, 16),
            "thruster-burn", new CommandCatalogue.Rule(CommandCriticality.CRITICAL,
                    EnumSet.of(MissionPhase.NOMINAL), Role.FLIGHT_DIRECTOR, 64)));

    private final CommandAuthorisationService service =
            new CommandAuthorisationService(catalogue, new CurrentActor(), properties());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorisesACatalogueCommandForTheRequiredRole() {
        authenticateAs("director", Role.FLIGHT_DIRECTOR);

        CommandCatalogue.Rule rule = service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("thruster-burn", CommandCriticality.CRITICAL, NOW, null), NOW);

        assertThat(rule.payloadBytes()).isEqualTo(64);
    }

    @Test
    void authorisesAnAdministratorForAnyCommand() {
        authenticateAs("admin", Role.ADMIN);

        assertThat(service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("thruster-burn", CommandCriticality.CRITICAL, NOW, null), NOW)).isNotNull();
    }

    @Test
    void refusesAnOpcodeOutsideTheCatalogue() {
        authenticateAs("director", Role.FLIGHT_DIRECTOR);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("self-destruct", CommandCriticality.CRITICAL, NOW, null), NOW))
                .isInstanceOf(CommandNotPermittedException.class)
                .hasMessageContaining("command catalogue");
    }

    @Test
    void refusesACriticalityThatDisagreesWithTheCatalogue() {
        authenticateAs("director", Role.FLIGHT_DIRECTOR);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("thruster-burn", CommandCriticality.ROUTINE, NOW, null), NOW))
                .isInstanceOf(CommandNotPermittedException.class)
                .hasMessageContaining("classified CRITICAL");
    }

    @Test
    void refusesACommandThatIsNotAllowedInTheCurrentMissionPhase() {
        authenticateAs("director", Role.FLIGHT_DIRECTOR);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.SAFE_MODE),
                request("thruster-burn", CommandCriticality.CRITICAL, NOW, null), NOW))
                .isInstanceOf(CommandNotPermittedException.class)
                .hasMessageContaining("not permitted in mission phase SAFE_MODE");
    }

    @Test
    void refusesAnyCommandOnceTheSpacecraftIsDecommissioned() {
        authenticateAs("admin", Role.ADMIN);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.DECOMMISSIONED),
                request("ping", CommandCriticality.ROUTINE, NOW, null), NOW))
                .isInstanceOf(CommandNotPermittedException.class)
                .hasMessageContaining("accepts no commands");
    }

    @Test
    void refusesACallerWithoutTheRequiredRole() {
        authenticateAs("operator", Role.OPERATOR);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("thruster-burn", CommandCriticality.CRITICAL, NOW, null), NOW))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("FLIGHT_DIRECTOR");
    }

    @Test
    void refusesACommandWhoseTimestampIsOutsideTheAcceptedSkew() {
        authenticateAs("operator", Role.OPERATOR);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("ping", CommandCriticality.ROUTINE, NOW.minus(Duration.ofMinutes(10)), null), NOW))
                .isInstanceOf(CommandStaleException.class)
                .hasMessageContaining("300 seconds");
    }

    @Test
    void acceptsACommandInsideTheAcceptedSkew() {
        authenticateAs("operator", Role.OPERATOR);

        assertThat(service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("ping", CommandCriticality.ROUTINE, NOW.minus(Duration.ofMinutes(4)), null), NOW))
                .isNotNull();
    }

    @Test
    void refusesAnExecutionTimeThatPrecedesTheRequest() {
        authenticateAs("operator", Role.OPERATOR);

        assertThatThrownBy(() -> service.authorise(spacecraft(MissionPhase.NOMINAL),
                request("ping", CommandCriticality.ROUTINE, NOW, NOW.minus(Duration.ofMinutes(1))), NOW))
                .isInstanceOf(CommandNotPermittedException.class)
                .hasMessageContaining("executeAfter");
    }

    private void authenticateAs(String username, Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role.authority()))));
    }

    private Spacecraft spacecraft(MissionPhase phase) {
        Spacecraft spacecraft = new Spacecraft("TESTSAT", 1, new byte[]{1}, new byte[]{1}, NOW);
        if (phase != MissionPhase.COMMISSIONING) {
            spacecraft.transitionTo(phase);
        }
        return spacecraft;
    }

    private SubmitCommandRequest request(String opcode, CommandCriticality criticality, Instant requestedAt,
                                         Instant executeAfter) {
        return new SubmitCommandRequest(opcode, criticality, Map.of(), "nonce-0123456789abcdef",
                requestedAt, executeAfter, "c2lnbmF0dXJl");
    }

    private GroundSegmentProperties properties() {
        return new GroundSegmentProperties(
                new GroundSegmentProperties.Jwt("integration-test-signing-key-0123456789abcdef",
                        Duration.ofMinutes(15), "test"),
                new GroundSegmentProperties.Crypto("5wwTGlnv/8J0FwEGgViotGHCMy1/uLscwsQQcT5CmEA="),
                new GroundSegmentProperties.Telemetry(64, 4096, 2047),
                new GroundSegmentProperties.Uplink(20, Duration.ofMinutes(5)));
    }
}
