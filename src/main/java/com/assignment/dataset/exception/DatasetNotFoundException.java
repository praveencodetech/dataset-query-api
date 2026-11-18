package com.assignment.dataset.exception;

/**
 * Thrown when querying a dataset that does not exist or has no records (maps to 404 Not Found).
 */
public class DatasetNotFoundException extends RuntimeException {

    public DatasetNotFoundException(String message) {
        super(message);
    }
}
