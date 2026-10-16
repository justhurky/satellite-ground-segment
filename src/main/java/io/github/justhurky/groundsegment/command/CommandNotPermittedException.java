package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.common.error.ErrorCode;
import io.github.justhurky.groundsegment.common.error.GroundSegmentException;

public class CommandNotPermittedException extends GroundSegmentException {

    public CommandNotPermittedException(String message) {
        super(ErrorCode.COMMAND_NOT_PERMITTED, message);
    }
}
