package io.github.justhurky.groundsegment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.justhurky.groundsegment.command.CommandCanonicaliser;
import io.github.justhurky.groundsegment.command.CommandCriticality;
import io.github.justhurky.groundsegment.command.UplinkPassService;
import io.github.justhurky.groundsegment.command.dto.SubmitCommandRequest;
import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.crypto.Ed25519Keys;
import io.github.justhurky.groundsegment.crypto.FrameAuthenticator;
import io.github.justhurky.groundsegment.groundstation.dto.RegisterGroundStationRequest;
import io.github.justhurky.groundsegment.groundstation.dto.ScheduleContactWindowRequest;
import io.github.justhurky.groundsegment.identity.AppUserRepository;
import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.identity.UserService;
import io.github.justhurky.groundsegment.identity.dto.CreateUserRequest;
import io.github.justhurky.groundsegment.spacecraft.MissionPhase;
import io.github.justhurky.groundsegment.spacecraft.dto.MissionPhaseChangeRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.RegisterSpacecraftRequest;
import io.github.justhurky.groundsegment.support.MissionTestClient;
import io.github.justhurky.groundsegment.support.TestFrames;
import io.github.justhurky.groundsegment.telemetry.CommandAcknowledgement;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestRequest;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MissionOperationsIntegrationTest {

    private static final String PASSWORD = "integration-test-password";
    private static final int TELEMETRY_APID = 100;
    private static final int ACKNOWLEDGEMENT_APID = 2047;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private CommandCanonicaliser canonicaliser;

    @Autowired
    private UplinkPassService uplinkPassService;

    private MissionTestClient client;
    private final FrameAuthenticator frameAuthenticator = new FrameAuthenticator();
    private final KeyPair missionKeys = Ed25519Keys.generate();

    @BeforeEach
    void prepare() {
        client = new MissionTestClient(mockMvc, objectMapper);
        if (userRepository.count() == 0) {
            userService.create(new CreateUserRequest("admin", PASSWORD, EnumSet.of(Role.ADMIN)));
            userService.create(new CreateUserRequest("director", PASSWORD, EnumSet.of(Role.FLIGHT_DIRECTOR)));
            userService.create(new CreateUserRequest("operator", PASSWORD, EnumSet.of(Role.OPERATOR)));
            userService.create(new CreateUserRequest("analyst", PASSWORD, EnumSet.of(Role.ANALYST)));
            userService.create(new CreateUserRequest("auditor", PASSWORD, EnumSet.of(Role.AUDITOR)));
            userService.create(new CreateUserRequest("station", PASSWORD, EnumSet.of(Role.GROUND_STATION)));
        }
    }

    @Test
    void carriesAMissionFromTelemetryIngestToAnAcknowledgedCommand() throws Exception {
        Mission mission = commissionedMission();
        String stationToken = client.login("station", PASSWORD);
        String operatorToken = client.login("operator", PASSWORD);

        Instant generatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Map<Integer, Double> samples = new LinkedHashMap<>();
        samples.put(1, 27.4);
        samples.put(3, 61.0);
        JsonNode ingest = client.postJson("/api/v1/telemetry/frames", stationToken,
                ingestRequest(mission, TELEMETRY_APID, 1, generatedAt, TestFrames.samplePayload(samples)), 201);

        assertThat(ingest.get("sampleCount").asInt()).isEqualTo(2);
        assertThat(ingest.get("raisedAlerts").size()).isZero();

        JsonNode frames = client.getJson(
                "/api/v1/spacecraft/" + mission.spacecraftId() + "/telemetry/frames", operatorToken, 200);
        assertThat(frames.get("totalElements").asLong()).isEqualTo(1);

        JsonNode series = client.getJson("/api/v1/spacecraft/" + mission.spacecraftId()
                + "/telemetry/parameters/BATTERY_VOLTAGE/series", operatorToken, 200);
        assertThat(series.get(0).get("observedValue").asDouble()).isEqualTo(27.4);
        assertThat(series.get(0).get("unit").asText()).isEqualTo("V");

        JsonNode submitted = client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands",
                operatorToken, signedCommand(mission, "ping", CommandCriticality.ROUTINE), 201);
        UUID commandId = UUID.fromString(submitted.get("id").asText());
        assertThat(submitted.get("state").asText()).isEqualTo("VALIDATED");

        UplinkPassService.PassReport report = uplinkPassService.executePass(mission.spacecraftId());
        assertThat(report.hadContact()).isTrue();
        assertThat(report.uplinked()).isEqualTo(1);

        assertThat(client.getJson("/api/v1/commands/" + commandId, operatorToken, 200)
                .get("state").asText()).isEqualTo("UPLINKED");

        byte[] acknowledgement = new CommandAcknowledgement(commandId, true,
                CommandAcknowledgement.RESULT_CODE_EXECUTED).encode();
        client.postJson("/api/v1/telemetry/frames", stationToken,
                ingestRequest(mission, ACKNOWLEDGEMENT_APID, 1, generatedAt, acknowledgement), 201);

        JsonNode completed = client.getJson("/api/v1/commands/" + commandId, operatorToken, 200);
        assertThat(completed.get("state").asText()).isEqualTo("ACKNOWLEDGED");
        assertThat(completed.get("resultCode").asInt()).isZero();
    }

    @Test
    void raisesARedAlertWhenAParameterLeavesItsLimits() throws Exception {
        Mission mission = commissionedMission();
        String stationToken = client.login("station", PASSWORD);
        String operatorToken = client.login("operator", PASSWORD);

        JsonNode ingest = client.postJson("/api/v1/telemetry/frames", stationToken,
                ingestRequest(mission, TELEMETRY_APID, 1, Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        TestFrames.samplePayload(Map.of(1, 20.5))), 201);

        assertThat(ingest.get("raisedAlerts").size()).isEqualTo(1);
        JsonNode alert = ingest.get("raisedAlerts").get(0);
        assertThat(alert.get("severity").asText()).isEqualTo("RED");
        assertThat(alert.get("limitKind").asText()).isEqualTo("RED_LOW");
        assertThat(alert.get("parameterName").asText()).isEqualTo("BATTERY_VOLTAGE");

        JsonNode acknowledged = client.postJson(
                "/api/v1/alerts/" + alert.get("id").asText() + "/acknowledge", operatorToken, null, 200);
        assertThat(acknowledged.get("acknowledged").asBoolean()).isTrue();
        assertThat(acknowledged.get("acknowledgedBy").asText()).isEqualTo("operator");
    }

    @Test
    void rejectsATelemetryFrameWhoseAuthenticationTagDoesNotMatch() throws Exception {
        Mission mission = commissionedMission();
        String stationToken = client.login("station", PASSWORD);

        byte[] frame = TestFrames.telemetryFrame(TELEMETRY_APID, 1, Instant.now(),
                TestFrames.samplePayload(Map.of(1, 27.4)));
        FrameIngestRequest forged = new FrameIngestRequest(mission.spacecraftId(), mission.stationCode(),
                Codecs.toBase64(frame), Codecs.toBase64(new byte[FrameAuthenticator.TAG_LENGTH_BYTES]));

        JsonNode problem = client.postJson("/api/v1/telemetry/frames", stationToken, forged, 401);

        assertThat(problem.get("errorCode").asText()).isEqualTo("FRAME_AUTHENTICATION_FAILED");
    }

    @Test
    void rejectsAReplayedTelemetryFrame() throws Exception {
        Mission mission = commissionedMission();
        String stationToken = client.login("station", PASSWORD);
        FrameIngestRequest request = ingestRequest(mission, TELEMETRY_APID, 7,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), TestFrames.samplePayload(Map.of(1, 27.4)));

        client.postJson("/api/v1/telemetry/frames", stationToken, request, 201);
        JsonNode problem = client.postJson("/api/v1/telemetry/frames", stationToken, request, 409);

        assertThat(problem.get("errorCode").asText()).isEqualTo("FRAME_REPLAY_DETECTED");
    }

    @Test
    void rejectsAFrameThatViolatesTheCcsdsStructure() throws Exception {
        Mission mission = commissionedMission();
        String stationToken = client.login("station", PASSWORD);

        byte[] frame = TestFrames.frame(0, 0, 0, TELEMETRY_APID, 1, Instant.now(), new byte[9], 0);
        JsonNode problem = client.postJson("/api/v1/telemetry/frames", stationToken,
                signedFrame(mission, frame), 400);

        assertThat(problem.get("errorCode").asText()).isEqualTo("FRAME_MALFORMED");
    }

    @Test
    void storesACommandWithABrokenSignatureAsRejected() throws Exception {
        Mission mission = commissionedMission();
        String operatorToken = client.login("operator", PASSWORD);

        SubmitCommandRequest valid = signedCommand(mission, "ping", CommandCriticality.ROUTINE);
        byte[] signature = Codecs.fromBase64(valid.signatureBase64());
        signature[0] = (byte) (signature[0] ^ 0x01);
        SubmitCommandRequest tampered = new SubmitCommandRequest(valid.opcode(), valid.criticality(),
                valid.parameters(), valid.nonce(), valid.requestedAt(), valid.executeAfter(),
                Codecs.toBase64(signature));

        JsonNode rejected = client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands",
                operatorToken, tampered, 422);

        assertThat(rejected.get("state").asText()).isEqualTo("REJECTED");
        assertThat(rejected.get("outcomeDetail").asText()).isEqualTo("signature-verification-failed");
    }

    @Test
    void refusesACriticalCommandSubmittedByAnOperator() throws Exception {
        Mission mission = commissionedMission();
        String directorToken = client.login("director", PASSWORD);
        String operatorToken = client.login("operator", PASSWORD);
        client.putJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/mission-phase", directorToken,
                new MissionPhaseChangeRequest(MissionPhase.NOMINAL, "commissioning complete"), 200);

        JsonNode problem = client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands",
                operatorToken, signedCommand(mission, "thruster-burn", CommandCriticality.CRITICAL), 403);

        assertThat(problem.get("errorCode").asText()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void acceptsACriticalCommandSubmittedByTheFlightDirector() throws Exception {
        Mission mission = commissionedMission();
        String directorToken = client.login("director", PASSWORD);
        client.putJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/mission-phase", directorToken,
                new MissionPhaseChangeRequest(MissionPhase.NOMINAL, "commissioning complete"), 200);

        JsonNode accepted = client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands",
                directorToken, signedCommand(mission, "thruster-burn", CommandCriticality.CRITICAL), 201);

        assertThat(accepted.get("state").asText()).isEqualTo("VALIDATED");
        assertThat(accepted.get("criticality").asText()).isEqualTo("CRITICAL");
        assertThat(accepted.get("requestedBy").asText()).isEqualTo("director");
    }

    @Test
    void refusesAnOpcodeThatTheMissionPhaseDoesNotAllow() throws Exception {
        Mission mission = commissionedMission();
        String directorToken = client.login("director", PASSWORD);

        JsonNode problem = client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands",
                directorToken, signedCommand(mission, "exit-safe-mode", CommandCriticality.CRITICAL), 422);

        assertThat(problem.get("errorCode").asText()).isEqualTo("COMMAND_NOT_PERMITTED");
    }

    @Test
    void refusesADuplicateCommandNonce() throws Exception {
        Mission mission = commissionedMission();
        String operatorToken = client.login("operator", PASSWORD);
        SubmitCommandRequest request = signedCommand(mission, "ping", CommandCriticality.ROUTINE);

        client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands", operatorToken,
                request, 201);
        JsonNode problem = client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/commands",
                operatorToken, request, 409);

        assertThat(problem.get("errorCode").asText()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    void refusesAnonymousAccessToTelemetry() throws Exception {
        Mission mission = commissionedMission();

        JsonNode problem = client.getJson(
                "/api/v1/spacecraft/" + mission.spacecraftId() + "/telemetry/frames", null, 401);

        assertThat(problem.get("errorCode").asText()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void refusesAnAnalystTheRightToScheduleContactWindows() throws Exception {
        Mission mission = commissionedMission();
        String analystToken = client.login("analyst", PASSWORD);

        client.postJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/contact-windows", analystToken,
                new ScheduleContactWindowRequest(mission.stationCode(), Instant.now(),
                        Instant.now().plus(Duration.ofHours(1)), 1024), 403);
    }

    @Test
    void keepsTheAuditTrailVerifiableAfterOperations() throws Exception {
        Mission mission = commissionedMission();
        String stationToken = client.login("station", PASSWORD);
        String auditorToken = client.login("auditor", PASSWORD);

        client.postJson("/api/v1/telemetry/frames", stationToken,
                ingestRequest(mission, TELEMETRY_APID, 1, Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        TestFrames.samplePayload(Map.of(1, 20.0))), 201);

        JsonNode report = client.getJson("/api/v1/audit/integrity", auditorToken, 200);

        assertThat(report.get("intact").asBoolean()).isTrue();
        assertThat(report.get("eventCount").asInt()).isPositive();
        assertThat(report.get("headHash").asText()).matches("[0-9a-f]{64}");
    }

    @Test
    void movesTheMissionPhaseOnlyAlongPermittedTransitions() throws Exception {
        Mission mission = commissionedMission();
        String directorToken = client.login("director", PASSWORD);
        String operatorToken = client.login("operator", PASSWORD);

        JsonNode nominal = client.putJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/mission-phase",
                directorToken, new MissionPhaseChangeRequest(MissionPhase.NOMINAL, "commissioning complete"),
                200);
        assertThat(nominal.get("missionPhase").asText()).isEqualTo("NOMINAL");

        client.putJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/mission-phase", operatorToken,
                new MissionPhaseChangeRequest(MissionPhase.SAFE_MODE, "not my call"), 403);

        client.putJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/mission-phase", directorToken,
                new MissionPhaseChangeRequest(MissionPhase.DECOMMISSIONED, "end of life"), 200);
        JsonNode problem = client.putJson("/api/v1/spacecraft/" + mission.spacecraftId() + "/mission-phase",
                directorToken, new MissionPhaseChangeRequest(MissionPhase.NOMINAL, "reactivate"), 409);

        assertThat(problem.get("errorCode").asText()).isEqualTo("ILLEGAL_STATE_TRANSITION");
    }

    private Mission commissionedMission() throws Exception {
        String adminToken = client.login("admin", PASSWORD);
        String operatorToken = client.login("operator", PASSWORD);
        int ordinal = SEQUENCE.getAndIncrement();
        String spacecraftName = "TESTSAT-" + ordinal;
        String stationCode = "ST" + ordinal;

        JsonNode registration = client.postJson("/api/v1/spacecraft", adminToken,
                new RegisterSpacecraftRequest(spacecraftName, 40000 + ordinal,
                        Codecs.toBase64(missionKeys.getPublic().getEncoded())), 201);
        UUID spacecraftId = UUID.fromString(registration.get("spacecraft").get("id").asText());
        byte[] telemetryKey = Codecs.fromBase64(
                registration.get("telemetryAuthenticationKeyBase64").asText());

        client.postJson("/api/v1/spacecraft/" + spacecraftId + "/parameters", adminToken,
                new ParameterDefinitionRequest(1, "BATTERY_VOLTAGE", "V", 22.0, 24.0, 32.0, 34.0), 201);
        client.postJson("/api/v1/spacecraft/" + spacecraftId + "/parameters", adminToken,
                new ParameterDefinitionRequest(3, "PANEL_TEMPERATURE", "C", -45.0, -30.0, 70.0, 85.0), 201);

        client.postJson("/api/v1/ground-stations", adminToken,
                new RegisterGroundStationRequest(stationCode, "Station " + ordinal, 47.47, 19.05, 120.0), 201);
        client.postJson("/api/v1/spacecraft/" + spacecraftId + "/contact-windows", operatorToken,
                new ScheduleContactWindowRequest(stationCode, Instant.now().minus(Duration.ofMinutes(1)),
                        Instant.now().plus(Duration.ofHours(2)), 65536), 201);

        return new Mission(spacecraftId, stationCode, telemetryKey);
    }

    private FrameIngestRequest ingestRequest(Mission mission, int apid, int sequenceCount, Instant generatedAt,
                                             byte[] payload) {
        return signedFrame(mission, TestFrames.telemetryFrame(apid, sequenceCount, generatedAt, payload));
    }

    private FrameIngestRequest signedFrame(Mission mission, byte[] frame) {
        return new FrameIngestRequest(mission.spacecraftId(), mission.stationCode(), Codecs.toBase64(frame),
                Codecs.toBase64(frameAuthenticator.tag(mission.telemetryKey(), frame)));
    }

    private SubmitCommandRequest signedCommand(Mission mission, String opcode, CommandCriticality criticality) {
        Instant requestedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String nonce = "nonce-" + UUID.randomUUID().toString().replace("-", "");
        Map<String, String> parameters = Map.of("reason", "integration-test");
        String canonical = canonicaliser.canonicalise(mission.spacecraftId(), opcode, criticality, nonce,
                requestedAt, parameters);
        byte[] signature = Ed25519Keys.sign(missionKeys.getPrivate().getEncoded(),
                canonicaliser.signedBytes(canonical));
        return new SubmitCommandRequest(opcode, criticality, parameters, nonce, requestedAt, null,
                Codecs.toBase64(signature));
    }

    private record Mission(UUID spacecraftId, String stationCode, byte[] telemetryKey) {
    }
}
