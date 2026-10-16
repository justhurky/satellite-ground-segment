package io.github.justhurky.groundsegment.telemetry.dto;

import io.github.justhurky.groundsegment.telemetry.Alert;
import io.github.justhurky.groundsegment.telemetry.AlertSeverity;
import io.github.justhurky.groundsegment.telemetry.LimitBreach;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID spacecraftId,
        long frameId,
        String parameterName,
        double observedValue,
        AlertSeverity severity,
        LimitBreach.LimitKind limitKind,
        double limitValue,
        Instant raisedAt,
        boolean acknowledged,
        String acknowledgedBy,
        Instant acknowledgedAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getSpacecraft().getId(),
                alert.getFrameId(),
                alert.getParameterName(),
                alert.getObservedValue(),
                alert.getSeverity(),
                alert.getLimitKind(),
                alert.getLimitValue(),
                alert.getRaisedAt(),
                alert.isAcknowledged(),
                alert.getAcknowledgedBy(),
                alert.getAcknowledgedAt());
    }
}
