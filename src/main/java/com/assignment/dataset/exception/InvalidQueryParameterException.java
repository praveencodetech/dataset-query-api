package com.assignment.dataset.exception;

/**
 * Thrown when an invalid query parameter value (e.g. invalid sort order) is supplied.
 */
public class InvalidQueryParameterException extends RuntimeException {

    public InvalidQueryParameterException(String message) {
        super(message);
    }
}
