package io.github.justhurky.groundsegment.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.crypto.Ed25519Keys;
import io.github.justhurky.groundsegment.groundstation.ContactWindowRepository;
import io.github.justhurky.groundsegment.groundstation.GroundStationService;
import io.github.justhurky.groundsegment.groundstation.dto.ContactWindowResponse;
import io.github.justhurky.groundsegment.groundstation.dto.RegisterGroundStationRequest;
import io.github.justhurky.groundsegment.groundstation.dto.ScheduleContactWindowRequest;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.spacecraft.dto.RegisterSpacecraftRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UplinkPassServiceTest {

    private static final AtomicInteger ORDINAL = new AtomicInteger(1);

    @Autowired
    private UplinkPassService uplinkPassService;

    @Autowired
    private TelecommandRepository telecommandRepository;

    @Autowired
    private SpacecraftService spacecraftService;

    @Autowired
    private GroundStationService groundStationService;

    @Autowired
    private ContactWindowRepository contactWindowRepository;

    private Spacecraft spacecraft;
    private String stationCode;

    @BeforeEach
    void registerMission() {
        int ordinal = ORDINAL.getAndIncrement();
        stationCode = "UP" + ordinal;
        UUID spacecraftId = spacecraftService.register(new RegisterSpacecraftRequest(
                "UPSAT-" + ordinal, 60000 + ordinal,
                Codecs.toBase64(Ed25519Keys.generate().getPublic().getEncoded()))).spacecraft().id();
        spacecraft = spacecraftService.require(spacecraftId);
        groundStationService.register(new RegisterGroundStationRequest(
                stationCode, "Uplink station " + ordinal, 47.47, 19.05, 120.0));
    }

    @Test
    void reportsNoContactWhenNoWindowIsOpen() {
        Telecommand command = validatedCommand(CommandCriticality.ROUTINE, 16, null);

        UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraft.getId());

        assertThat(report.hadContact()).isFalse();
        assertThat(report.uplinked()).isZero();
        assertThat(stateOf(command)).isEqualTo(CommandState.VALIDATED);
    }

    @Test
    void uplinksADueCommandDuringAnOpenWindow() {
        openWindow(1024);
        Telecommand command = validatedCommand(CommandCriticality.ROUTINE, 16, null);

        UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraft.getId());

        assertThat(report.hadContact()).isTrue();
        assertThat(report.uplinked()).isEqualTo(1);
        assertThat(report.failed()).isZero();
        assertThat(stateOf(command)).isEqualTo(CommandState.UPLINKED);
    }

    @Test
    void recordsTheWindowItUsedAndTheBytesItSpent() {
        ContactWindowResponse window = openWindow(1024);
        Telecommand command = validatedCommand(CommandCriticality.ROUTINE, 24, null);

        uplinkPassService.executePass(spacecraft.getId());

        Telecommand uplinked = telecommandRepository.findById(command.getId()).orElseThrow();
        assertThat(uplinked.getContactWindowId()).isEqualTo(window.id());
        assertThat(uplinked.getScheduledAt()).isNotNull();
        assertThat(uplinked.getUplinkedAt()).isNotNull();
        assertThat(contactWindowRepository.findById(window.id()).orElseThrow().getUplinkedBytes())
                .isEqualTo(24);
    }

    @Test
    void uplinksTheMoreCriticalCommandFirstWhenTheBudgetIsTight() {
        openWindow(64);
        Telecommand routine = validatedCommand(CommandCriticality.ROUTINE, 16, null);
        Telecommand critical = validatedCommand(CommandCriticality.CRITICAL, 64, null);

        UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraft.getId());

        assertThat(stateOf(critical)).isEqualTo(CommandState.UPLINKED);
        assertThat(stateOf(routine)).isEqualTo(CommandState.VALIDATED);
        assertThat(report.uplinked()).isEqualTo(1);
        assertThat(report.deferred()).isEqualTo(1);
    }

    @Test
    void defersACommandThatDoesNotFitTheRemainingBudget() {
        openWindow(32);
        Telecommand oversized = validatedCommand(CommandCriticality.CRITICAL, 64, null);

        UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraft.getId());

        assertThat(report.deferred()).isEqualTo(1);
        assertThat(report.uplinked()).isZero();
        assertThat(stateOf(oversized)).isEqualTo(CommandState.VALIDATED);
    }

    @Test
    void leavesACommandWaitingUntilItsExecutionTime() {
        openWindow(1024);
        Telecommand scheduledForLater = validatedCommand(CommandCriticality.ROUTINE, 16,
                Instant.now().plus(Duration.ofHours(2)));

        UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraft.getId());

        assertThat(report.hadContact()).isTrue();
        assertThat(report.uplinked()).isZero();
        assertThat(stateOf(scheduledForLater)).isEqualTo(CommandState.VALIDATED);
    }

    @Test
    void ignoresCommandsThatAreNoLongerWaitingToBeSent() {
        openWindow(1024);
        Telecommand cancelled = validatedCommand(CommandCriticality.ROUTINE, 16, null);
        cancelled.cancel("operator changed the plan", Instant.now());
        telecommandRepository.save(cancelled);

        UplinkPassService.PassReport report = uplinkPassService.executePass(spacecraft.getId());

        assertThat(report.uplinked()).isZero();
        assertThat(stateOf(cancelled)).isEqualTo(CommandState.CANCELLED);
    }

    private ContactWindowResponse openWindow(int budgetBytes) {
        return groundStationService.scheduleWindow(spacecraft.getId(), new ScheduleContactWindowRequest(
                stationCode, Instant.now().minus(Duration.ofMinutes(1)),
                Instant.now().plus(Duration.ofHours(1)), budgetBytes));
    }

    private Telecommand validatedCommand(CommandCriticality criticality, int payloadBytes,
                                         Instant executeAfter) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Telecommand command = new Telecommand(spacecraft, "PING", criticality, "",
                "nonce-" + UUID.randomUUID().toString().replace("-", ""), now, now, executeAfter,
                new byte[64], "0".repeat(64), payloadBytes, "operator");
        command.markValidated();
        return telecommandRepository.save(command);
    }

    private CommandState stateOf(Telecommand command) {
        return telecommandRepository.findById(command.getId()).orElseThrow().getState();
    }
}
