package com.assignment.dataset.exception;

/**
 * Thrown when the record payload is empty, malformed, or not a JSON object (maps to 400 Bad Request).
 */
public class InvalidRecordException extends RuntimeException {

    public InvalidRecordException(String message) {
        super(message);
    }

    public InvalidRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
