package com.assignment.dataset.strategy;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

/**
 * Dynamic comparator for record maps by a designated field name.
 * Handles Numbers (numeric comparison), Strings (case-insensitive), Booleans,
 * and mixed types predictably.
 *
 * Missing or null values are always positioned last (nulls-last) for both ASC and DESC orders.
 */
public class DynamicFieldComparator implements Comparator<Map<String, Object>> {

    private final String fieldName;
    private final SortOrder order;

    public DynamicFieldComparator(String fieldName, SortOrder order) {
        this.fieldName = fieldName;
        this.order = (order != null) ? order : SortOrder.ASC;
    }

    @Override
    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
        Object v1 = (o1 != null) ? o1.get(fieldName) : null;
        Object v2 = (o2 != null) ? o2.get(fieldName) : null;

        // Nulls last in both directions
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return 1;
        }
        if (v2 == null) {
            return -1;
        }

        int comparison = compareNonNullValues(v1, v2);

        return (order == SortOrder.DESC) ? -comparison : comparison;
    }

    /**
     * Compares two non-null values based on their runtime types.
     */
    private int compareNonNullValues(Object v1, Object v2) {
        // Both are Numbers
        if (v1 instanceof Number n1 && v2 instanceof Number n2) {
            try {
                BigDecimal b1 = new BigDecimal(n1.toString());
                BigDecimal b2 = new BigDecimal(n2.toString());
                return b1.compareTo(b2);
            } catch (NumberFormatException e) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            }
        }

        // Both are Booleans
        if (v1 instanceof Boolean b1 && v2 instanceof Boolean b2) {
            return Boolean.compare(b1, b2);
        }

        // Both are Strings
        if (v1 instanceof String s1 && v2 instanceof String s2) {
            return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
        }

        // Mixed types: fallback to case-insensitive string comparison
        return String.CASE_INSENSITIVE_ORDER.compare(v1.toString(), v2.toString());
    }
}
