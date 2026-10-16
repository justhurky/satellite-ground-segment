package io.github.justhurky.groundsegment.spacecraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.justhurky.groundsegment.common.error.IllegalStateTransitionException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class MissionPhaseTest {

    @ParameterizedTest
    @CsvSource({
            "COMMISSIONING,NOMINAL",
            "COMMISSIONING,SAFE_MODE",
            "COMMISSIONING,DECOMMISSIONED",
            "NOMINAL,SAFE_MODE",
            "NOMINAL,DECOMMISSIONED",
            "SAFE_MODE,NOMINAL",
            "SAFE_MODE,DECOMMISSIONED"
    })
    void permitsTheOperationalTransitions(MissionPhase from, MissionPhase to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "NOMINAL,COMMISSIONING",
            "SAFE_MODE,COMMISSIONING",
            "DECOMMISSIONED,NOMINAL",
            "DECOMMISSIONED,SAFE_MODE",
            "DECOMMISSIONED,COMMISSIONING"
    })
    void refusesToReturnToAnEarlierPhase(MissionPhase from, MissionPhase to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MissionPhase.class, names = {"COMMISSIONING", "NOMINAL", "SAFE_MODE"})
    void acceptsCommandingWhileTheMissionIsAlive(MissionPhase phase) {
        assertThat(phase.acceptsCommanding()).isTrue();
    }

    @Test
    void refusesCommandingOnceDecommissioned() {
        assertThat(MissionPhase.DECOMMISSIONED.acceptsCommanding()).isFalse();
        assertThat(MissionPhase.DECOMMISSIONED.allowedTargets()).isEmpty();
    }

    @Test
    void appliesTheTransitionRulesOnTheSpacecraft() {
        Spacecraft spacecraft = spacecraft();

        spacecraft.transitionTo(MissionPhase.NOMINAL);
        spacecraft.transitionTo(MissionPhase.SAFE_MODE);

        assertThat(spacecraft.getMissionPhase()).isEqualTo(MissionPhase.SAFE_MODE);
    }

    @Test
    void toleratesATransitionToTheCurrentPhase() {
        Spacecraft spacecraft = spacecraft();

        spacecraft.transitionTo(MissionPhase.COMMISSIONING);

        assertThat(spacecraft.getMissionPhase()).isEqualTo(MissionPhase.COMMISSIONING);
    }

    @Test
    void rejectsAForbiddenTransitionOnTheSpacecraft() {
        Spacecraft spacecraft = spacecraft();
        spacecraft.transitionTo(MissionPhase.DECOMMISSIONED);

        assertThatThrownBy(() -> spacecraft.transitionTo(MissionPhase.NOMINAL))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("DECOMMISSIONED");
    }

    private Spacecraft spacecraft() {
        return new Spacecraft("TESTSAT", 1, new byte[]{1}, new byte[]{1}, Instant.EPOCH);
    }
}
