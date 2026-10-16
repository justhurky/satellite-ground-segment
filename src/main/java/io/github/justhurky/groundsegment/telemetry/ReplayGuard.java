package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.config.GroundSegmentProperties;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplayGuard {

    private final SequenceStateRepository sequenceStateRepository;
    private final int windowSize;
    private final Clock clock;

    public ReplayGuard(SequenceStateRepository sequenceStateRepository, GroundSegmentProperties properties,
                       Clock clock) {
        this.sequenceStateRepository = sequenceStateRepository;
        this.windowSize = properties.telemetry().replayWindowSize();
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void admit(UUID spacecraftId, int apid, int sequenceCount) {
        sequenceStateRepository.findBySpacecraftIdAndApid(spacecraftId, apid)
                .ifPresentOrElse(
                        state -> advance(state, sequenceCount),
                        () -> sequenceStateRepository.save(new SequenceState(spacecraftId, apid,
                                ReplayWindow.startingAt(sequenceCount), clock.instant())));
    }

    private void advance(SequenceState state, int sequenceCount) {
        ReplayWindow accepted = state.window().accept(sequenceCount, windowSize)
                .orElseThrow(() -> new ReplayDetectedException("Sequence count " + sequenceCount
                        + " on APID " + state.getApid() + " was already received or is outside the replay window"));
        state.advanceTo(accepted, clock.instant());
    }
}
