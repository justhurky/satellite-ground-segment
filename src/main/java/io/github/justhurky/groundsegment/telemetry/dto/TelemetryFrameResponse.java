package io.github.justhurky.groundsegment.telemetry.dto;

import io.github.justhurky.groundsegment.telemetry.TelemetryFrame;
import java.time.Instant;

public record TelemetryFrameResponse(
        long id,
        int apid,
        int sequenceCount,
        int sequenceFlags,
        String groundStationCode,
        Instant generatedAt,
        Instant receivedAt,
        int payloadLength) {

    public static TelemetryFrameResponse from(TelemetryFrame frame) {
        return new TelemetryFrameResponse(
                frame.getId(),
                frame.getApid(),
                frame.getSequenceCount(),
                frame.getSequenceFlags(),
                frame.getGroundStation().getCode(),
                frame.getGeneratedAt(),
                frame.getReceivedAt(),
                frame.getPayloadLength());
    }
}
