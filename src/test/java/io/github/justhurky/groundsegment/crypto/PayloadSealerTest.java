package io.github.justhurky.groundsegment.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PayloadSealerTest {

    private static final String CONTEXT = "telemetry-payload:spacecraft-1:100";
    private static final byte[] PLAINTEXT = "battery voltage 27.4V".getBytes(StandardCharsets.UTF_8);

    private final PayloadSealer sealer = new PayloadSealer(properties(
            "5wwTGlnv/8J0FwEGgViotGHCMy1/uLscwsQQcT5CmEA="));

    @Test
    void sealsAndUnsealsWithTheSameContext() {
        byte[] sealed = sealer.seal(PLAINTEXT, CONTEXT);

        assertThat(sealer.unseal(sealed, CONTEXT)).isEqualTo(PLAINTEXT);
    }

    @Test
    void producesADifferentCiphertextOnEveryCall() {
        assertThat(sealer.seal(PLAINTEXT, CONTEXT)).isNotEqualTo(sealer.seal(PLAINTEXT, CONTEXT));
    }

    @Test
    void prefixesTheCiphertextWithTheNonce() {
        byte[] sealed = sealer.seal(PLAINTEXT, CONTEXT);

        assertThat(sealed).hasSize(PayloadSealer.NONCE_LENGTH_BYTES + PLAINTEXT.length
                + PayloadSealer.TAG_LENGTH_BITS / 8);
    }

    @Test
    void refusesToUnsealWithADifferentContext() {
        byte[] sealed = sealer.seal(PLAINTEXT, CONTEXT);

        assertThatThrownBy(() -> sealer.unseal(sealed, "telemetry-payload:spacecraft-2:100"))
                .isInstanceOf(SealedPayloadException.class)
                .hasMessageContaining("authentication tag");
    }

    @Test
    void refusesToUnsealATamperedCiphertext() {
        byte[] sealed = sealer.seal(PLAINTEXT, CONTEXT);
        sealed[sealed.length - 1] = (byte) (sealed[sealed.length - 1] ^ 0x01);

        assertThatThrownBy(() -> sealer.unseal(sealed, CONTEXT))
                .isInstanceOf(SealedPayloadException.class);
    }

    @Test
    void refusesToUnsealAPayloadShorterThanTheNonce() {
        byte[] truncated = Arrays.copyOf(sealer.seal(PLAINTEXT, CONTEXT), PayloadSealer.NONCE_LENGTH_BYTES);

        assertThatThrownBy(() -> sealer.unseal(truncated, CONTEXT))
                .isInstanceOf(SealedPayloadException.class)
                .hasMessageContaining("shorter than the nonce");
    }

    @Test
    void rejectsAKeyOfTheWrongLength() {
        assertThatThrownBy(() -> new PayloadSealer(properties("c2hvcnQta2V5")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("16 or 32 bytes");
    }

    private GroundSegmentProperties properties(String dataEncryptionKey) {
        return new GroundSegmentProperties(
                new GroundSegmentProperties.Jwt("test-signing-key-0123456789abcdef", Duration.ofMinutes(15),
                        "test"),
                new GroundSegmentProperties.Crypto(dataEncryptionKey),
                new GroundSegmentProperties.Telemetry(64, 4096, 2047),
                new GroundSegmentProperties.Uplink(20, Duration.ofMinutes(5)));
    }
}
