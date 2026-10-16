package io.github.justhurky.groundsegment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.justhurky.groundsegment.audit.AuditIntegrityService;
import io.github.justhurky.groundsegment.bootstrap.DemoDataSeeder;
import io.github.justhurky.groundsegment.bootstrap.DemoProperties;
import io.github.justhurky.groundsegment.command.CommandCatalogue;
import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.crypto.FrameAuthenticator;
import io.github.justhurky.groundsegment.groundstation.GroundStationService;
import io.github.justhurky.groundsegment.identity.AppUserRepository;
import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftRepository;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoProfileSeedingIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private SpacecraftService spacecraftService;

    @Autowired
    private GroundStationService groundStationService;

    @Autowired
    private AuditIntegrityService auditIntegrityService;

    @Autowired
    private CommandCatalogue commandCatalogue;

    @Autowired
    private DemoProperties demoProperties;

    @Test
    void seedsOneAccountPerOperationalRole() {
        assertThat(userRepository.count()).isEqualTo(6);
        assertThat(userRepository.findByUsername("director").orElseThrow().getRoles())
                .containsExactly(Role.FLIGHT_DIRECTOR);
        assertThat(userRepository.findByUsername("station-kir").orElseThrow().getRoles())
                .containsExactly(Role.GROUND_STATION);
    }

    @Test
    void seedsACommissionedSpacecraftWithItsParameterDictionary() {
        Spacecraft spacecraft = spacecraftRepository.findByName(DemoDataSeeder.SPACECRAFT_NAME).orElseThrow();

        assertThat(spacecraft.getNoradId()).isEqualTo(DemoDataSeeder.SPACECRAFT_NORAD_ID);
        assertThat(spacecraft.getMissionPhase()).isEqualTo(MissionPhase.COMMISSIONING);
        assertThat(spacecraftService.parametersOf(spacecraft.getId()))
                .hasSize(6)
                .anySatisfy(parameter -> {
                    assertThat(parameter.name()).isEqualTo("BATTERY_VOLTAGE");
                    assertThat(parameter.redLow()).isEqualTo(22.0);
                });
    }

    @Test
    void registersTheSpacecraftWithTheDemoCommandVerificationKey() {
        Spacecraft spacecraft = spacecraftRepository.findByName(DemoDataSeeder.SPACECRAFT_NAME).orElseThrow();

        assertThat(Codecs.toBase64(spacecraft.getCommandPublicKey()))
                .isEqualTo(demoProperties.commandPublicKeyBase64());
    }

    @Test
    void sealsAUsableTelemetryKeyForTheSeededSpacecraft() {
        Spacecraft spacecraft = spacecraftRepository.findByName(DemoDataSeeder.SPACECRAFT_NAME).orElseThrow();

        assertThat(spacecraftService.telemetryKeyOf(spacecraft))
                .hasSize(FrameAuthenticator.KEY_LENGTH_BYTES);
    }

    @Test
    void opensAContactWindowThatTheDispatcherCanUse() {
        Spacecraft spacecraft = spacecraftRepository.findByName(DemoDataSeeder.SPACECRAFT_NAME).orElseThrow();

        assertThat(groundStationService.findAll()).hasSize(2);
        assertThat(groundStationService.openWindow(spacecraft.getId(), Instant.now())).isPresent();
    }

    @Test
    void loadsTheConfiguredCommandCatalogue() {
        assertThat(commandCatalogue.opcodes())
                .contains("PING", "ENTER_SAFE_MODE", "EXIT_SAFE_MODE", "THRUSTER_BURN");
        assertThat(commandCatalogue.ruleFor("exit-safe-mode").orElseThrow().allowedPhases())
                .containsExactly(MissionPhase.SAFE_MODE);
    }

    @Test
    void leavesTheSeededAuditTrailIntact() {
        assertThat(auditIntegrityService.verify().intact()).isTrue();
    }
}
