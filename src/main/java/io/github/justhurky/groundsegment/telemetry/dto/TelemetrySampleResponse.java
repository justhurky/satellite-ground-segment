package io.github.justhurky.groundsegment.telemetry.dto;

import io.github.justhurky.groundsegment.telemetry.TelemetrySample;
import java.time.Instant;

public record TelemetrySampleResponse(
        int parameterSlot,
        String parameterName,
        double observedValue,
        String unit,
        boolean mapped,
        Instant generatedAt) {

    public static TelemetrySampleResponse from(TelemetrySample sample) {
        return new TelemetrySampleResponse(
                sample.getParameterSlot(),
                sample.getParameterName(),
                sample.getObservedValue(),
                sample.getUnit(),
                sample.isMapped(),
                sample.getGeneratedAt());
    }
}
