package io.github.justhurky.groundsegment.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommandAcknowledgementTest {

    private static final UUID COMMAND_ID = UUID.fromString("7f7d9a2e-5c51-4b7a-9c1c-0a39f6f1b2d4");

    @Test
    void roundTripsAnExecutedAcknowledgement() {
        CommandAcknowledgement original = new CommandAcknowledgement(COMMAND_ID, true,
                CommandAcknowledgement.RESULT_CODE_EXECUTED);

        assertThat(CommandAcknowledgement.decode(original.encode())).isEqualTo(original);
    }

    @Test
    void marksANonZeroResultCodeAsNotExecuted() {
        byte[] payload = new CommandAcknowledgement(COMMAND_ID, false, 7).encode();

        CommandAcknowledgement decoded = CommandAcknowledgement.decode(payload);

        assertThat(decoded.executed()).isFalse();
        assertThat(decoded.resultCode()).isEqualTo(7);
    }

    @Test
    void encodesExactlySeventeenBytes() {
        assertThat(new CommandAcknowledgement(COMMAND_ID, true, 0).encode())
                .hasSize(CommandAcknowledgement.PAYLOAD_BYTES);
    }

    @Test
    void rejectsAPayloadOfTheWrongLength() {
        assertThatThrownBy(() -> CommandAcknowledgement.decode(new byte[16]))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("17 bytes");
    }
}
