package io.github.justhurky.groundsegment.crypto;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class FrameAuthenticator {

    public static final String ALGORITHM = "HmacSHA256";
    public static final int KEY_LENGTH_BYTES = 32;
    public static final int TAG_LENGTH_BYTES = 32;

    public byte[] tag(byte[] key, byte[] frame) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(frame);
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Unable to compute an " + ALGORITHM + " tag", e);
        }
    }

    public boolean matches(byte[] key, byte[] frame, byte[] presentedTag) {
        if (presentedTag == null || presentedTag.length != TAG_LENGTH_BYTES) {
            return false;
        }
        return MessageDigest.isEqual(tag(key, frame), presentedTag);
    }
}
