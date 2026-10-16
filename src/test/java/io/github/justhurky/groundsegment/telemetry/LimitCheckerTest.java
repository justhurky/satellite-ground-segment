package io.github.justhurky.groundsegment.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.justhurky.groundsegment.spacecraft.ParameterDefinition;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LimitCheckerTest {

    private final LimitChecker limitChecker = new LimitChecker();
    private final ParameterDefinition batteryVoltage = definition(22.0, 24.0, 32.0, 34.0);

    @Test
    void reportsNoBreachInsideTheGreenBand() {
        assertThat(limitChecker.evaluate(batteryVoltage, 27.5)).isEmpty();
    }

    @Test
    void reportsARedBreachAtOrBelowTheRedLowLimit() {
        Optional<LimitBreach> breach = limitChecker.evaluate(batteryVoltage, 22.0);

        assertThat(breach).contains(new LimitBreach(AlertSeverity.RED, LimitBreach.LimitKind.RED_LOW, 22.0));
    }

    @Test
    void reportsARedBreachAtOrAboveTheRedHighLimit() {
        assertThat(limitChecker.evaluate(batteryVoltage, 40.0))
                .contains(new LimitBreach(AlertSeverity.RED, LimitBreach.LimitKind.RED_HIGH, 34.0));
    }

    @Test
    void reportsAYellowBreachBetweenTheYellowAndRedLowLimits() {
        assertThat(limitChecker.evaluate(batteryVoltage, 23.5))
                .contains(new LimitBreach(AlertSeverity.YELLOW, LimitBreach.LimitKind.YELLOW_LOW, 24.0));
    }

    @Test
    void reportsAYellowBreachBetweenTheYellowAndRedHighLimits() {
        assertThat(limitChecker.evaluate(batteryVoltage, 33.0))
                .contains(new LimitBreach(AlertSeverity.YELLOW, LimitBreach.LimitKind.YELLOW_HIGH, 32.0));
    }

    @Test
    void prefersTheRedBreachWhenBothBandsApply() {
        assertThat(limitChecker.evaluate(batteryVoltage, 10.0).orElseThrow().severity())
                .isEqualTo(AlertSeverity.RED);
    }

    @Test
    void ignoresLimitsThatAreNotConfigured() {
        ParameterDefinition onlyLowLimits = definition(3.0, 6.0, null, null);

        assertThat(limitChecker.evaluate(onlyLowLimits, 120.0)).isEmpty();
        assertThat(limitChecker.evaluate(onlyLowLimits, 3.0)).isPresent();
    }

    @Test
    void treatsANotANumberReadingAsARedBreach() {
        assertThat(limitChecker.evaluate(batteryVoltage, Double.NaN).orElseThrow().severity())
                .isEqualTo(AlertSeverity.RED);
    }

    private ParameterDefinition definition(Double redLow, Double yellowLow, Double yellowHigh, Double redHigh) {
        Spacecraft spacecraft = new Spacecraft("TESTSAT", 1, new byte[]{1}, new byte[]{1}, Instant.EPOCH);
        return new ParameterDefinition(spacecraft, 1, "BATTERY_VOLTAGE", "V",
                redLow, yellowLow, yellowHigh, redHigh);
    }
}
