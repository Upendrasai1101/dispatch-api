package com.upendra.dispatch.exception;

import com.upendra.dispatch.dto.ErrorResponse;
import com.upendra.dispatch.dto.FieldErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

/**
 * Turns every exception into the same JSON error shape:
 * {"status":"error","message":"...","errors":[{"field":"...","message":"..."}],"timestamp":"..."}
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorResponse(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorResponse::field)
                        .thenComparing(FieldErrorResponse::message))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(DuplicateIdException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateIdException ex) {
        List<FieldErrorResponse> errors = ex.getDuplicateIds().stream()
                .map(id -> new FieldErrorResponse(ex.getField(), "duplicate value '" + id + "'"))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Malformed JSON request or invalid value type. Check the body is valid JSON "
                        + "and numeric fields contain numbers.", List.of());
    }

    @ExceptionHandler(DispatchDataMissingException.class)
    public ResponseEntity<ErrorResponse> handleMissingData(DispatchDataMissingException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Required query parameter '" + ex.getParameterName() + "' is missing",
                List.of(new FieldErrorResponse(ex.getParameterName(), "is required")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint", List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type must be application/json", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource not found: /" + ex.getResourcePath(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                List<FieldErrorResponse> errors) {
        return ResponseEntity.status(status).body(ErrorResponse.of(message, errors));
    }
}
