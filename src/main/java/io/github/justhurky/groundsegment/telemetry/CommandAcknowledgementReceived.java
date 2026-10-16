package io.github.justhurky.groundsegment.telemetry;

import java.time.Instant;
import java.util.UUID;

public record CommandAcknowledgementReceived(
        UUID commandId,
        boolean executed,
        int resultCode,
        long frameId,
        Instant receivedAt) {
}
