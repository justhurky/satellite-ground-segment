package io.github.justhurky.groundsegment.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.justhurky.groundsegment.common.error.IllegalStateTransitionException;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelecommandTest {

    private static final Instant NOW = Instant.parse("2026-03-14T09:26:53Z");

    @Test
    void startsInTheReceivedState() {
        assertThat(command().getState()).isEqualTo(CommandState.RECEIVED);
    }

    @Test
    void carriesThePriorityOfItsCriticality() {
        assertThat(command().getPriority()).isEqualTo(CommandCriticality.CRITICAL.priority());
    }

    @Test
    void walksTheHappyPathToAcknowledged() {
        Telecommand command = command();
        UUID windowId = UUID.randomUUID();

        command.markValidated();
        command.schedule(windowId, NOW);
        command.markUplinked(NOW.plusSeconds(1));
        command.markAcknowledged(0, NOW.plusSeconds(30));

        assertThat(command.getState()).isEqualTo(CommandState.ACKNOWLEDGED);
        assertThat(command.getContactWindowId()).isEqualTo(windowId);
        assertThat(command.getUplinkedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(command.getCompletedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(command.getResultCode()).isZero();
    }

    @Test
    void refusesToUplinkACommandThatWasNeverScheduled() {
        Telecommand command = command();
        command.markValidated();

        assertThatThrownBy(() -> command.markUplinked(NOW))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("VALIDATED");
    }

    @Test
    void refusesToActOnATerminalCommand() {
        Telecommand command = command();
        command.reject("signature-verification-failed");

        assertThatThrownBy(command::markValidated)
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(command.getOutcomeDetail()).isEqualTo("signature-verification-failed");
    }

    @Test
    void recordsTheReasonWhenCancelled() {
        Telecommand command = command();
        command.markValidated();
        command.cancel("superseded by a later plan", NOW);

        assertThat(command.getState()).isEqualTo(CommandState.CANCELLED);
        assertThat(command.getOutcomeDetail()).isEqualTo("superseded by a later plan");
        assertThat(command.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotExposeTheStoredSignatureArray() {
        Telecommand command = command();
        byte[] exported = command.getSignature();
        exported[0] = (byte) 0xff;

        assertThat(command.getSignature()).isNotEqualTo(exported);
    }

    private Telecommand command() {
        Spacecraft spacecraft = new Spacecraft("TESTSAT", 1, new byte[]{1}, new byte[]{1}, NOW);
        return new Telecommand(spacecraft, "THRUSTER_BURN", CommandCriticality.CRITICAL,
                "duration=12;thrust=0.4", "nonce-0123456789abcdef", NOW, NOW, null, new byte[64],
                "0".repeat(64), 64, "director");
    }
}
