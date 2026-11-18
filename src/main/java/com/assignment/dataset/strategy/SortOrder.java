package com.assignment.dataset.strategy;

import com.assignment.dataset.exception.InvalidQueryParameterException;

/**
 * Represents sort direction (ASC or DESC).
 */
public enum SortOrder {
    ASC,
    DESC;

    /**
     * Parses the string into a SortOrder.
     *
     * @param value the string representation (asc or desc, case-insensitive)
     * @return the matching SortOrder
     * @throws InvalidQueryParameterException if value is not asc or desc
     */
    public static SortOrder fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ASC;
        }
        String normalized = value.trim().toLowerCase();
        if ("asc".equals(normalized)) {
            return ASC;
        }
        if ("desc".equals(normalized)) {
            return DESC;
        }
        throw new InvalidQueryParameterException(
                "Invalid order parameter '" + value + "'. Allowed values are 'asc' or 'desc'."
        );
    }
}
