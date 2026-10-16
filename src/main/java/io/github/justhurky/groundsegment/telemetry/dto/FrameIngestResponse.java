package io.github.justhurky.groundsegment.telemetry.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FrameIngestResponse(
        long frameId,
        UUID spacecraftId,
        int apid,
        int sequenceCount,
        Instant generatedAt,
        Instant receivedAt,
        int sampleCount,
        int unmappedSampleCount,
        List<AlertResponse> raisedAlerts,
        UUID acknowledgedCommandId) {
}
