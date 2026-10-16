package io.github.justhurky.groundsegment.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Digests {

    public static final String SHA_256 = "SHA-256";

    private Digests() {
    }

    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(SHA_256 + " is not available in this runtime", e);
        }
    }

    public static String sha256Hex(byte[] input) {
        return Codecs.toHex(sha256().digest(input));
    }

    public static String sha256Hex(String input) {
        return sha256Hex(input.getBytes(StandardCharsets.UTF_8));
    }
}
