package com.upendra.dispatch.exception;

import java.util.List;

/** The same id appears more than once inside a single request. */
public class DuplicateIdException extends RuntimeException {

    private final String field;
    private final List<String> duplicateIds;

    public DuplicateIdException(String field, List<String> duplicateIds) {
        super("Duplicate " + field + " values in request: " + duplicateIds);
        this.field = field;
        this.duplicateIds = List.copyOf(duplicateIds);
    }

    public String getField() {
        return field;
    }

    public List<String> getDuplicateIds() {
        return duplicateIds;
    }
}
