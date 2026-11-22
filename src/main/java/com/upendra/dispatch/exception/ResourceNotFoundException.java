package com.upendra.dispatch.exception;

/** A specific order or vehicle id does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
