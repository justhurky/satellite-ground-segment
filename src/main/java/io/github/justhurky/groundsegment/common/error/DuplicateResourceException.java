package io.github.justhurky.groundsegment.common.error;

public class DuplicateResourceException extends GroundSegmentException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
