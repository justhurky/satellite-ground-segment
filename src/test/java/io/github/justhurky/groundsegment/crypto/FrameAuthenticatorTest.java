package io.github.justhurky.groundsegment.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FrameAuthenticatorTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER_KEY = "fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FRAME = "telemetry-frame-payload".getBytes(StandardCharsets.UTF_8);

    private final FrameAuthenticator authenticator = new FrameAuthenticator();

    @Test
    void producesAThirtyTwoByteTagDeterministically() {
        byte[] first = authenticator.tag(KEY, FRAME);
        byte[] second = authenticator.tag(KEY, FRAME);

        assertThat(first).hasSize(FrameAuthenticator.TAG_LENGTH_BYTES).isEqualTo(second);
    }

    @Test
    void acceptsATagProducedWithTheSameKey() {
        assertThat(authenticator.matches(KEY, FRAME, authenticator.tag(KEY, FRAME))).isTrue();
    }

    @Test
    void rejectsATagProducedWithAnotherKey() {
        assertThat(authenticator.matches(KEY, FRAME, authenticator.tag(OTHER_KEY, FRAME))).isFalse();
    }

    @Test
    void rejectsATagWhenTheFrameWasModified() {
        byte[] tag = authenticator.tag(KEY, FRAME);
        byte[] tampered = FRAME.clone();
        tampered[0] = (byte) (tampered[0] ^ 0x01);

        assertThat(authenticator.matches(KEY, tampered, tag)).isFalse();
    }

    @Test
    void rejectsATagOfTheWrongLength() {
        byte[] truncated = Arrays.copyOf(authenticator.tag(KEY, FRAME), 16);

        assertThat(authenticator.matches(KEY, FRAME, truncated)).isFalse();
    }

    @Test
    void rejectsAMissingTag() {
        assertThat(authenticator.matches(KEY, FRAME, null)).isFalse();
    }
}
