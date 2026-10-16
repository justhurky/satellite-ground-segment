package io.github.justhurky.groundsegment.common.util;

import java.util.Base64;
import java.util.HexFormat;

public final class Codecs {

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final HexFormat HEX = HexFormat.of();

    private Codecs() {
    }

    public static byte[] fromBase64(String value) {
        return DECODER.decode(value);
    }

    public static String toBase64(byte[] value) {
        return ENCODER.encodeToString(value);
    }

    public static String toHex(byte[] value) {
        return HEX.formatHex(value);
    }

    public static byte[] fromHex(String value) {
        return HEX.parseHex(value);
    }
}
