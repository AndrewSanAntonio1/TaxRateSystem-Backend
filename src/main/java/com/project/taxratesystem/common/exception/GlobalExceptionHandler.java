package com.project.taxratesystem.common.exception;

import com.project.taxratesystem.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception into the single error envelope of API.md §5.
 *
 * <p>Rules encoded here:
 * <ul>
 *   <li>{@code fields} appears only for field-level failures;</li>
 *   <li>{@code message} is safe to display - never a password, hash, token or SQL fragment;</li>
 *   <li>{@code timestamp} is ISO-8601 UTC and {@code path} is the request URI without query;</li>
 *   <li>{@code 500} responses carry a generic message, never a stack trace or class name.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_ERROR = "Validation Error";
    private static final String GENERIC_SERVER_ERROR = "Something went wrong. Please try again later.";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        HttpStatus status = exception.getStatus();
        String label = exception.isFieldLevel() ? VALIDATION_ERROR : status.getReasonPhrase();
        ErrorResponse body = exception.isFieldLevel()
                ? ErrorResponse.ofFields(status.value(), label, exception.getMessage(), request.getRequestURI(),
                        exception.getFields())
                : ErrorResponse.of(status.value(), label, exception.getMessage(), request.getRequestURI());

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (exception instanceof TooManyRequestsException throttled) {
            builder.header(HttpHeaders.RETRY_AFTER, Long.toString(throttled.getRetryAfterSeconds()));
        }
        return builder.body(body);
    }

    /** Bean validation on a request DTO - {@code 422} with one entry per offending field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException exception,
                                                              HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), messageOf(error));
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            fields.putIfAbsent(error.getObjectName(), messageOf(error));
        }
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.ofFields(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), VALIDATION_ERROR, "Invalid request",
                request.getRequestURI(), fields));
    }

    /** Bean validation on query/path parameters - {@code 400} per API.md §13.6. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(ConstraintViolationException exception,
                                                                   HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.ofFields(HttpStatus.BAD_REQUEST.value(),
                VALIDATION_ERROR, "Invalid request", request.getRequestURI(), fields));
    }

    /** A body that cannot be parsed or bound at all - {@code 400} with a {@code body} field. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                              HttpServletRequest request) {
        log.debug("Rejected unreadable request body on {}", request.getRequestURI());
        return handleApiException(ValidationException.malformedBody("Malformed request body"), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                            HttpServletRequest request) {
        return handleApiException(new ValidationException(HttpStatus.BAD_REQUEST, "Invalid request",
                Map.of(exception.getName(), "Invalid value")), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception,
                                                                HttpServletRequest request) {
        return handleApiException(new ValidationException(HttpStatus.BAD_REQUEST, "Invalid request",
                Map.of(exception.getParameterName(), "Required parameter is missing")), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(), HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                exception.getMessage(), request.getRequestURI()));
    }

    /** Unknown routes and other {@code ResponseStatusException}s raised by Spring MVC. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception,
                                                              HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(
                ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "Authentication required.", request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(), "Access denied.", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception,
                                                             HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(),
                exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(), "The request conflicts with existing data.",
                request.getRequestURI()));
    }

    /** Last resort - {@code 500} with a generic message; the detail stays in the server log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                GENERIC_SERVER_ERROR, request.getRequestURI()));
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
    }

    private static String messageOf(ObjectError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid request";
    }
}

