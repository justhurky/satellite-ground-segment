package io.github.justhurky.groundsegment.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    FRAME_MALFORMED(HttpStatus.BAD_REQUEST),
    FRAME_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED),
    FRAME_REPLAY_DETECTED(HttpStatus.CONFLICT),
    COMMAND_SIGNATURE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY),
    COMMAND_NOT_PERMITTED(HttpStatus.UNPROCESSABLE_ENTITY),
    COMMAND_STALE(HttpStatus.UNPROCESSABLE_ENTITY),
    ILLEGAL_STATE_TRANSITION(HttpStatus.CONFLICT),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    KEY_MATERIAL_UNUSABLE(HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public String type() {
        return "https://ground-segment.example/problems/" + name().toLowerCase().replace('_', '-');
    }
}
