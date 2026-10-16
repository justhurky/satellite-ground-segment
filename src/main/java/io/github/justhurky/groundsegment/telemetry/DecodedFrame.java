package io.github.justhurky.groundsegment.telemetry;

import java.time.Instant;

public record DecodedFrame(
        int packetVersion,
        int apid,
        int sequenceFlags,
        int sequenceCount,
        Instant generatedAt,
        byte[] payload) {

    public int payloadLength() {
        return payload.length;
    }
}
