package io.github.justhurky.groundsegment.common.error;

public class IllegalStateTransitionException extends GroundSegmentException {

    public IllegalStateTransitionException(String message) {
        super(ErrorCode.ILLEGAL_STATE_TRANSITION, message);
    }
}
