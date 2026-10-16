package io.github.justhurky.groundsegment.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class CommandStateTest {

    @ParameterizedTest
    @CsvSource({
            "RECEIVED,VALIDATED",
            "RECEIVED,REJECTED",
            "VALIDATED,SCHEDULED",
            "VALIDATED,CANCELLED",
            "VALIDATED,FAILED",
            "SCHEDULED,UPLINKED",
            "SCHEDULED,CANCELLED",
            "SCHEDULED,FAILED",
            "UPLINKED,ACKNOWLEDGED",
            "UPLINKED,FAILED"
    })
    void permitsTheDocumentedTransitions(CommandState from, CommandState to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "RECEIVED,SCHEDULED",
            "RECEIVED,UPLINKED",
            "RECEIVED,ACKNOWLEDGED",
            "VALIDATED,UPLINKED",
            "VALIDATED,ACKNOWLEDGED",
            "VALIDATED,REJECTED",
            "SCHEDULED,ACKNOWLEDGED",
            "SCHEDULED,VALIDATED",
            "UPLINKED,CANCELLED",
            "UPLINKED,SCHEDULED"
    })
    void refusesTransitionsThatSkipOrReverseTheLifecycle(CommandState from, CommandState to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CommandState.class,
            names = {"ACKNOWLEDGED", "FAILED", "REJECTED", "CANCELLED"})
    void treatsOutcomeStatesAsTerminal(CommandState state) {
        assertThat(state.isTerminal()).isTrue();
        assertThat(state.allowedTargets()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = CommandState.class, names = {"RECEIVED", "VALIDATED", "SCHEDULED", "UPLINKED"})
    void treatsInFlightStatesAsNonTerminal(CommandState state) {
        assertThat(state.isTerminal()).isFalse();
    }

    @Test
    void neverPermitsASelfTransition() {
        assertThat(Arrays.stream(CommandState.values()).filter(state -> state.canTransitionTo(state)))
                .isEmpty();
    }

    @Test
    void declaresEveryStateInTheTransitionTable() {
        assertThat(Arrays.stream(CommandState.values()).map(CommandState::allowedTargets))
                .allSatisfy(targets -> assertThat(targets).isNotNull());
    }
}
