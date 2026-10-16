package io.github.justhurky.groundsegment.telemetry;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TelemetryMetrics {

    public static final String FRAMES_ACCEPTED = "groundsegment.telemetry.frames.accepted";
    public static final String FRAMES_REJECTED = "groundsegment.telemetry.frames.rejected";
    public static final String ALERTS_RAISED = "groundsegment.telemetry.alerts.raised";

    private final MeterRegistry meterRegistry;

    public TelemetryMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void frameAccepted(int apid) {
        meterRegistry.counter(FRAMES_ACCEPTED, "apid", Integer.toString(apid)).increment();
    }

    public void frameRejected(String reason) {
        meterRegistry.counter(FRAMES_REJECTED, "reason", reason).increment();
    }

    public void alertRaised(AlertSeverity severity) {
        meterRegistry.counter(ALERTS_RAISED, "severity", severity.name()).increment();
    }
}
