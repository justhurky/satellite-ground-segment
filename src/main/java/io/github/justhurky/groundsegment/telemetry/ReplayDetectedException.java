package io.github.justhurky.groundsegment.telemetry;

import io.github.justhurky.groundsegment.common.error.ErrorCode;
import io.github.justhurky.groundsegment.common.error.GroundSegmentException;

public class ReplayDetectedException extends GroundSegmentException {

    public ReplayDetectedException(String message) {
        super(ErrorCode.FRAME_REPLAY_DETECTED, message);
    }
}
