package com.assignment.dataset.strategy;

import com.assignment.dataset.exception.FieldNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Strategy responsible for validating and grouping records by a designated field,
 * with optional sorting applied within each group.
 */
@Component
public class GroupByStrategy {

    private final SortByStrategy sortByStrategy;

    public GroupByStrategy(SortByStrategy sortByStrategy) {
        this.sortByStrategy = sortByStrategy;
    }

    /**
     * Groups records by the designated field, optionally sorting records inside each group.
     *
     * @param records   list of JSON record maps
     * @param groupBy   field name to group by
     * @param sortBy    optional field name to sort each group by
     * @param sortOrder optional sort direction (ASC/DESC)
     * @return map of group keys to lists of records
     * @throws FieldNotFoundException if groupBy or sortBy field does not exist in any record
     */
    public Map<String, List<Map<String, Object>>> group(
            List<Map<String, Object>> records,
            String groupBy,
            String sortBy,
            SortOrder sortOrder
    ) {
        if (records == null || records.isEmpty() || groupBy == null || groupBy.isBlank()) {
            return Collections.emptyMap();
        }

        validateFieldExists(records, groupBy);

        // Group into LinkedHashMap to preserve insertion / encounter order
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();

        for (Map<String, Object> record : records) {
            Object val = (record != null) ? record.get(groupBy) : null;
            String groupKey = (val != null) ? String.valueOf(val) : "null";
            grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(record);
        }

        // If sortBy is requested, sort the records inside each group
        if (sortBy != null && !sortBy.isBlank()) {
            // First validate that sortBy exists across the dataset
            sortByStrategy.validateFieldExists(records, sortBy);

            SortOrder order = (sortOrder != null) ? sortOrder : SortOrder.ASC;
            for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
                List<Map<String, Object>> sortedGroup = sortByStrategy.sort(entry.getValue(), sortBy, order);
                entry.setValue(sortedGroup);
            }
        }

        return grouped;
    }

    /**
     * Verifies that the group-by field exists in at least one record of the dataset.
     */
    public void validateFieldExists(List<Map<String, Object>> records, String fieldName) {
        boolean exists = records.stream()
                .anyMatch(record -> record != null && record.containsKey(fieldName));
        if (!exists) {
            throw new FieldNotFoundException(
                    "Field '" + fieldName + "' does not exist in the dataset."
            );
        }
    }
}
