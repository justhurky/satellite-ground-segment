package io.github.justhurky.groundsegment.telemetry;

public record LimitBreach(AlertSeverity severity, LimitKind limitKind, double threshold) {

    public enum LimitKind {
        RED_LOW,
        YELLOW_LOW,
        YELLOW_HIGH,
        RED_HIGH
    }
}
