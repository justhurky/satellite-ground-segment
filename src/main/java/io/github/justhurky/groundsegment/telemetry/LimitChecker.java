package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.spacecraft.ParameterDefinition;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LimitChecker {

    public Optional<LimitBreach> evaluate(ParameterDefinition definition, double value) {
        if (Double.isNaN(value)) {
            return Optional.of(new LimitBreach(AlertSeverity.RED, LimitBreach.LimitKind.RED_HIGH, Double.NaN));
        }
        if (definition.getRedLow() != null && value <= definition.getRedLow()) {
            return Optional.of(new LimitBreach(AlertSeverity.RED, LimitBreach.LimitKind.RED_LOW,
                    definition.getRedLow()));
        }
        if (definition.getRedHigh() != null && value >= definition.getRedHigh()) {
            return Optional.of(new LimitBreach(AlertSeverity.RED, LimitBreach.LimitKind.RED_HIGH,
                    definition.getRedHigh()));
        }
        if (definition.getYellowLow() != null && value <= definition.getYellowLow()) {
            return Optional.of(new LimitBreach(AlertSeverity.YELLOW, LimitBreach.LimitKind.YELLOW_LOW,
                    definition.getYellowLow()));
        }
        if (definition.getYellowHigh() != null && value >= definition.getYellowHigh()) {
            return Optional.of(new LimitBreach(AlertSeverity.YELLOW, LimitBreach.LimitKind.YELLOW_HIGH,
                    definition.getYellowHigh()));
        }
        return Optional.empty();
    }
}
