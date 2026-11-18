package com.assignment.dataset.exception;

/**
 * Thrown when a dataset name is blank or contains illegal characters (maps to 400 Bad Request).
 */
public class InvalidDatasetNameException extends RuntimeException {

    public InvalidDatasetNameException(String message) {
        super(message);
    }
}
