package io.github.justhurky.groundsegment.spacecraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ParameterDefinitionTest {

    private final Spacecraft spacecraft =
            new Spacecraft("TESTSAT", 1, new byte[]{1}, new byte[]{1}, Instant.EPOCH);

    @Test
    void acceptsAscendingThresholds() {
        ParameterDefinition definition = definition(22.0, 24.0, 32.0, 34.0);

        assertThat(definition.getRedLow()).isEqualTo(22.0);
        assertThat(definition.getRedHigh()).isEqualTo(34.0);
    }

    @Test
    void acceptsPartiallyConfiguredThresholds() {
        ParameterDefinition definition = definition(null, 6.0, null, 40.0);

        assertThat(definition.getRedLow()).isNull();
        assertThat(definition.getYellowLow()).isEqualTo(6.0);
    }

    @Test
    void refusesARedLowAboveTheYellowLow() {
        assertThatThrownBy(() -> definition(30.0, 24.0, 32.0, 34.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ascend");
    }

    @Test
    void refusesAYellowHighBelowTheYellowLow() {
        assertThatThrownBy(() -> definition(22.0, 30.0, 24.0, 34.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesARedHighBelowTheRedLow() {
        assertThatThrownBy(() -> definition(40.0, null, null, 10.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replacesThresholdsWhenLimitsAreRevised() {
        ParameterDefinition definition = definition(22.0, 24.0, 32.0, 34.0);

        definition.applyLimits(20.0, 23.0, 33.0, 36.0);

        assertThat(definition.getYellowLow()).isEqualTo(23.0);
        assertThat(definition.getRedHigh()).isEqualTo(36.0);
    }

    private ParameterDefinition definition(Double redLow, Double yellowLow, Double yellowHigh, Double redHigh) {
        return new ParameterDefinition(spacecraft, 1, "BATTERY_VOLTAGE", "V",
                redLow, yellowLow, yellowHigh, redHigh);
    }
}
