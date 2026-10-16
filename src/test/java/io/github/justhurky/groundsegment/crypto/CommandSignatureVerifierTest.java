package io.github.justhurky.groundsegment.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class CommandSignatureVerifierTest {

    private static final byte[] MESSAGE = "spacecraft-1\nTHRUSTER_BURN\nCRITICAL".getBytes(StandardCharsets.UTF_8);

    private final CommandSignatureVerifier verifier = new CommandSignatureVerifier();
    private final KeyPair missionKeys = Ed25519Keys.generate();

    @Test
    void acceptsASignatureFromTheMatchingPrivateKey() {
        byte[] signature = Ed25519Keys.sign(missionKeys.getPrivate().getEncoded(), MESSAGE);

        assertThat(verifier.verify(missionKeys.getPublic().getEncoded(), MESSAGE, signature)).isTrue();
    }

    @Test
    void rejectsASignatureOverADifferentMessage() {
        byte[] signature = Ed25519Keys.sign(missionKeys.getPrivate().getEncoded(), MESSAGE);
        byte[] otherMessage = "spacecraft-1\nPING\nROUTINE".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verify(missionKeys.getPublic().getEncoded(), otherMessage, signature)).isFalse();
    }

    @Test
    void rejectsASignatureFromAnotherKeyPair() {
        KeyPair attackerKeys = Ed25519Keys.generate();
        byte[] signature = Ed25519Keys.sign(attackerKeys.getPrivate().getEncoded(), MESSAGE);

        assertThat(verifier.verify(missionKeys.getPublic().getEncoded(), MESSAGE, signature)).isFalse();
    }

    @Test
    void rejectsASignatureWithFlippedBits() {
        byte[] signature = Ed25519Keys.sign(missionKeys.getPrivate().getEncoded(), MESSAGE);
        signature[0] = (byte) (signature[0] ^ 0x01);

        assertThat(verifier.verify(missionKeys.getPublic().getEncoded(), MESSAGE, signature)).isFalse();
    }

    @Test
    void rejectsASignatureOfTheWrongLength() {
        assertThat(verifier.verify(missionKeys.getPublic().getEncoded(), MESSAGE, new byte[8])).isFalse();
    }

    @Test
    void refusesAPublicKeyThatIsNotEd25519() {
        assertThatThrownBy(() -> verifier.verify(new byte[]{1, 2, 3}, MESSAGE, new byte[64]))
                .isInstanceOf(KeyMaterialException.class)
                .hasMessageContaining("public key");
    }
}
