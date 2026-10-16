package io.github.justhurky.groundsegment.crypto;

import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PayloadSealer {

    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int NONCE_LENGTH_BYTES = 12;
    public static final int TAG_LENGTH_BITS = 128;
    public static final int MAX_PLAINTEXT_BYTES = 1 << 20;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public PayloadSealer(GroundSegmentProperties properties) {
        byte[] keyBytes = Codecs.fromBase64(properties.crypto().dataEncryptionKey());
        if (keyBytes.length != 16 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "ground-segment.crypto.data-encryption-key must decode to 16 or 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] seal(byte[] plaintext, String associatedData) {
        if (plaintext.length > MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("Refusing to seal " + plaintext.length
                    + " bytes, the limit is " + MAX_PLAINTEXT_BYTES);
        }
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            byte[] sealed = new byte[Math.addExact(NONCE_LENGTH_BYTES,
                    cipher.getOutputSize(plaintext.length))];
            System.arraycopy(nonce, 0, sealed, 0, NONCE_LENGTH_BYTES);
            cipher.doFinal(plaintext, 0, plaintext.length, sealed, NONCE_LENGTH_BYTES);
            return sealed;
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Unable to seal a payload with " + TRANSFORMATION, e);
        }
    }

    public byte[] unseal(byte[] sealed, String associatedData) {
        if (sealed.length <= NONCE_LENGTH_BYTES) {
            throw new SealedPayloadException("Sealed payload is shorter than the nonce it must carry");
        }
        byte[] nonce = Arrays.copyOfRange(sealed, 0, NONCE_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(sealed, NONCE_LENGTH_BYTES, sealed.length);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new SealedPayloadException("Sealed payload failed its authentication tag check");
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Unable to unseal a payload with " + TRANSFORMATION, e);
        }
    }
}
