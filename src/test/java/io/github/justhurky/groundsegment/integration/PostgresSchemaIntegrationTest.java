package io.github.justhurky.groundsegment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.justhurky.groundsegment.audit.AuditIntegrityService;
import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.crypto.Ed25519Keys;
import io.github.justhurky.groundsegment.crypto.FrameAuthenticator;
import io.github.justhurky.groundsegment.groundstation.GroundStationService;
import io.github.justhurky.groundsegment.groundstation.dto.RegisterGroundStationRequest;
import io.github.justhurky.groundsegment.groundstation.dto.ScheduleContactWindowRequest;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.RegisterSpacecraftRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.SpacecraftRegistrationResponse;
import io.github.justhurky.groundsegment.support.TestFrames;
import io.github.justhurky.groundsegment.telemetry.AlertSeverity;
import io.github.justhurky.groundsegment.telemetry.ReplayDetectedException;
import io.github.justhurky.groundsegment.telemetry.TelemetryIngestService;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestRequest;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaIntegrationTest {

    private static final int TELEMETRY_APID = 120;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpacecraftService spacecraftService;

    @Autowired
    private GroundStationService groundStationService;

    @Autowired
    private TelemetryIngestService telemetryIngestService;

    @Autowired
    private AuditIntegrityService auditIntegrityService;

    private final FrameAuthenticator frameAuthenticator = new FrameAuthenticator();

    @Test
    void runsTheMigratedSchemaOnPostgresAndKeepsTheEntityMappingValid() {
        SpacecraftRegistrationResponse registration = spacecraftService.register(new RegisterSpacecraftRequest(
                "PGSAT-1", 51000, Codecs.toBase64(Ed25519Keys.generate().getPublic().getEncoded())));
        spacecraftService.defineParameter(registration.spacecraft().id(),
                new ParameterDefinitionRequest(1, "BATTERY_VOLTAGE", "V", 22.0, 24.0, 32.0, 34.0));
        groundStationService.register(new RegisterGroundStationRequest("PGS", "Postgres Station",
                47.47, 19.05, 120.0));
        groundStationService.scheduleWindow(registration.spacecraft().id(),
                new ScheduleContactWindowRequest("PGS", Instant.now().minus(Duration.ofMinutes(1)),
                        Instant.now().plus(Duration.ofHours(1)), 4096));

        byte[] telemetryKey = Codecs.fromBase64(registration.telemetryAuthenticationKeyBase64());
        FrameIngestResponse response = telemetryIngestService.ingest(ingestRequest(registration, telemetryKey,
                11, Map.of(1, 21.0)));

        assertThat(response.frameId()).isPositive();
        assertThat(response.sampleCount()).isEqualTo(1);
        assertThat(response.raisedAlerts()).singleElement()
                .satisfies(alert -> assertThat(alert.severity()).isEqualTo(AlertSeverity.RED));
        assertThat(auditIntegrityService.verify().intact()).isTrue();
    }

    @Test
    void enforcesTheReplayWindowAcrossTransactions() {
        SpacecraftRegistrationResponse registration = spacecraftService.register(new RegisterSpacecraftRequest(
                "PGSAT-2", 51001, Codecs.toBase64(Ed25519Keys.generate().getPublic().getEncoded())));
        groundStationService.register(new RegisterGroundStationRequest("PGT", "Postgres Tracking",
                47.47, 19.05, 120.0));
        byte[] telemetryKey = Codecs.fromBase64(registration.telemetryAuthenticationKeyBase64());

        telemetryIngestService.ingest(ingestRequest(registration, telemetryKey, 5, Map.of()));

        assertThatThrownBy(() -> telemetryIngestService.ingest(
                ingestRequest(registration, telemetryKey, 5, Map.of())))
                .isInstanceOf(ReplayDetectedException.class);
    }

    private FrameIngestRequest ingestRequest(SpacecraftRegistrationResponse registration, byte[] telemetryKey,
                                             int sequenceCount, Map<Integer, Double> samples) {
        String stationCode = registration.spacecraft().noradId() == 51000 ? "PGS" : "PGT";
        byte[] frame = TestFrames.telemetryFrame(TELEMETRY_APID, sequenceCount,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), TestFrames.samplePayload(samples));
        return new FrameIngestRequest(registration.spacecraft().id(), stationCode, Codecs.toBase64(frame),
                Codecs.toBase64(frameAuthenticator.tag(telemetryKey, frame)));
    }
}
