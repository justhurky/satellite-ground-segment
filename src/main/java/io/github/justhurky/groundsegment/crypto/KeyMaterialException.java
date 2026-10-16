package io.github.justhurky.groundsegment.crypto;

import io.github.justhurky.groundsegment.common.error.ErrorCode;
import io.github.justhurky.groundsegment.common.error.GroundSegmentException;

public class KeyMaterialException extends GroundSegmentException {

    public KeyMaterialException(String message, Throwable cause) {
        super(ErrorCode.KEY_MATERIAL_UNUSABLE, message, cause);
    }
}
