package com.upendra.dispatch.exception;

/** A plan was requested before any orders or vehicles were submitted. */
public class DispatchDataMissingException extends RuntimeException {

    public DispatchDataMissingException(String message) {
        super(message);
    }
}
