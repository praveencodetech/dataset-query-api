package com.assignment.dataset.exception;

/**
 * Thrown when a requested groupBy or sortBy field does not exist in any record of the dataset.
 */
public class FieldNotFoundException extends RuntimeException {

    public FieldNotFoundException(String message) {
        super(message);
    }
}
