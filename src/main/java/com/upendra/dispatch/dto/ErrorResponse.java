package com.upendra.dispatch.dto;

import java.time.Instant;
import java.util.List;

/** One error format for every failure the API returns. */
public record ErrorResponse(
        String status,
        String message,
        List<FieldErrorResponse> errors,
        String timestamp
) {

    public static ErrorResponse of(String message, List<FieldErrorResponse> errors) {
        return new ErrorResponse("error", message, List.copyOf(errors), Instant.now().toString());
    }

    public static ErrorResponse of(String message) {
        return of(message, List.of());
    }
}
