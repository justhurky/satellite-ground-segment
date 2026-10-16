package io.github.justhurky.groundsegment.crypto;

import io.github.justhurky.groundsegment.common.error.ErrorCode;
import io.github.justhurky.groundsegment.common.error.GroundSegmentException;

public class SealedPayloadException extends GroundSegmentException {

    public SealedPayloadException(String message) {
        super(ErrorCode.KEY_MATERIAL_UNUSABLE, message);
    }
}
