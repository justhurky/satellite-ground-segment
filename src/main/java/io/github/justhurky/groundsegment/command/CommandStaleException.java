package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.common.error.ErrorCode;
import io.github.justhurky.groundsegment.common.error.GroundSegmentException;

public class CommandStaleException extends GroundSegmentException {

    public CommandStaleException(String message) {
        super(ErrorCode.COMMAND_STALE, message);
    }
}
