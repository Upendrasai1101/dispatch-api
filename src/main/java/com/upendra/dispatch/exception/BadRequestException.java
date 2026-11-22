package com.upendra.dispatch.exception;

/** The request is well-formed JSON/HTTP but its content cannot be processed. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
