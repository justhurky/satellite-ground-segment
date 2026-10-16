package io.github.justhurky.groundsegment.common.error;

public class GroundSegmentException extends RuntimeException {

    private final ErrorCode errorCode;

    public GroundSegmentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GroundSegmentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
