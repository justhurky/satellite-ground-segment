package io.github.justhurky.groundsegment.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReplayWindowTest {

    private static final int WINDOW = 64;

    @Test
    void marksTheFirstSequenceAsSeen() {
        ReplayWindow window = ReplayWindow.startingAt(10);

        assertThat(window.lastSequence()).isEqualTo(10);
        assertThat(window.accept(10, WINDOW)).isEmpty();
    }

    @Test
    void acceptsStrictlyIncreasingSequences() {
        ReplayWindow window = ReplayWindow.startingAt(0);
        for (int sequence = 1; sequence <= 200; sequence++) {
            window = window.accept(sequence, WINDOW).orElseThrow();
        }

        assertThat(window.lastSequence()).isEqualTo(200);
    }

    @Test
    void acceptsAnOutOfOrderSequenceInsideTheWindow() {
        ReplayWindow window = ReplayWindow.startingAt(100).accept(105, WINDOW).orElseThrow();

        Optional<ReplayWindow> accepted = window.accept(102, WINDOW);

        assertThat(accepted).isPresent();
        assertThat(accepted.orElseThrow().lastSequence()).isEqualTo(105);
    }

    @Test
    void rejectsTheSameSequenceTwice() {
        ReplayWindow window = ReplayWindow.startingAt(100)
                .accept(105, WINDOW).orElseThrow()
                .accept(102, WINDOW).orElseThrow();

        assertThat(window.accept(102, WINDOW)).isEmpty();
    }

    @Test
    void rejectsASequenceOlderThanTheWindow() {
        ReplayWindow window = ReplayWindow.startingAt(0).accept(200, WINDOW).orElseThrow();

        assertThat(window.accept(100, WINDOW)).isEmpty();
    }

    @Test
    void rejectsSequencesThatHaveSlidOutOfTheWindow() {
        ReplayWindow window = ReplayWindow.startingAt(0).accept(10, 8).orElseThrow();

        assertThat(window.accept(2, 8)).isEmpty();
        assertThat(window.accept(3, 8)).isPresent();
        assertThat(window.accept(9, 8)).isPresent();
    }

    @Test
    void handlesSequenceCounterWraparound() {
        ReplayWindow window = ReplayWindow.startingAt(ReplayWindow.SEQUENCE_SPACE - 2);

        ReplayWindow wrapped = window.accept(1, WINDOW).orElseThrow();

        assertThat(wrapped.lastSequence()).isEqualTo(1);
        assertThat(wrapped.accept(ReplayWindow.SEQUENCE_SPACE - 1, WINDOW)).isPresent();
        assertThat(wrapped.accept(ReplayWindow.SEQUENCE_SPACE - 2, WINDOW)).isEmpty();
    }

    @Test
    void clearsTheMaskWhenTheJumpExceedsTheMaskWidth() {
        ReplayWindow window = ReplayWindow.startingAt(0).accept(5, WINDOW).orElseThrow();

        ReplayWindow jumped = window.accept(500, WINDOW).orElseThrow();

        assertThat(jumped.seenMask()).isEqualTo(1L);
    }

    @Test
    void clampsTheWindowSizeToTheMaskWidth() {
        ReplayWindow window = ReplayWindow.startingAt(0).accept(500, ReplayWindow.MAX_WINDOW_SIZE * 4)
                .orElseThrow();

        assertThat(window.accept(500 - ReplayWindow.MAX_WINDOW_SIZE, ReplayWindow.MAX_WINDOW_SIZE * 4)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, ReplayWindow.SEQUENCE_SPACE, ReplayWindow.SEQUENCE_SPACE + 1})
    void refusesSequencesOutsideTheFourteenBitRange(int sequence) {
        ReplayWindow window = ReplayWindow.startingAt(0);

        assertThatThrownBy(() -> window.accept(sequence, WINDOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 bit range");
    }
}
