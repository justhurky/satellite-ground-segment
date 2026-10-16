package io.github.justhurky.groundsegment.common.error;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(GroundSegmentException.class)
    public ProblemDetail onGroundSegmentException(GroundSegmentException exception, WebRequest request) {
        if (exception.errorCode().status().is5xxServerError()) {
            log.error("Unhandled domain failure on {}", path(request), exception);
        } else {
            log.warn("Rejected request on {}: {} {}", path(request), exception.errorCode(),
                    exception.getMessage());
        }
        return problem(exception.errorCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail onConstraintViolation(ConstraintViolationException exception, WebRequest request) {
        return problem(ErrorCode.VALIDATION_FAILED, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onIllegalArgument(IllegalArgumentException exception, WebRequest request) {
        return problem(ErrorCode.VALIDATION_FAILED, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail onAuthenticationFailure(AuthenticationException exception, WebRequest request) {
        log.warn("Authentication failed on {}: {}", path(request), exception.getMessage());
        return problem(ErrorCode.AUTHENTICATION_FAILED, "Authentication failed", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail onAccessDenied(AccessDeniedException exception, WebRequest request) {
        log.warn("Access denied on {}: {}", path(request), exception.getMessage());
        return problem(ErrorCode.ACCESS_DENIED, "Caller is not authorised for this operation", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpectedException(Exception exception, WebRequest request) {
        log.error("Unexpected failure on {}", path(request), exception);
        return problem(ErrorCode.INTERNAL_ERROR, "Unexpected server failure", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> violations = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> violations.put(error.getField(), error.getDefaultMessage()));
        ProblemDetail detail = problem(ErrorCode.VALIDATION_FAILED, "Request payload failed validation", request);
        detail.setProperty("violations", violations);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status()).body(detail);
    }

    private ProblemDetail problem(ErrorCode code, String message, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(code.status(), message);
        detail.setType(URI.create(code.type()));
        detail.setTitle(code.name());
        detail.setInstance(URI.create(path(request)));
        detail.setProperty("errorCode", code.name());
        detail.setProperty("timestamp", clock.instant().toString());
        return detail;
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}
