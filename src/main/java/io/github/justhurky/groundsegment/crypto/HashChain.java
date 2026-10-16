package io.github.justhurky.groundsegment.crypto;

import io.github.justhurky.groundsegment.common.util.Codecs;
import io.github.justhurky.groundsegment.common.util.Digests;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

@Component
public class HashChain {

    public static final String GENESIS_HASH = "0".repeat(64);

    private static final byte FIELD_SEPARATOR = 0x1f;

    public String link(String previousHash, String canonicalRecord) {
        MessageDigest digest = Digests.sha256();
        digest.update(previousHash.getBytes(StandardCharsets.UTF_8));
        digest.update(FIELD_SEPARATOR);
        digest.update(canonicalRecord.getBytes(StandardCharsets.UTF_8));
        return Codecs.toHex(digest.digest());
    }
}
