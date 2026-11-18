package com.assignment.dataset.strategy;

import com.assignment.dataset.exception.FieldNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy responsible for validating field existence and sorting records dynamically.
 */
@Component
public class SortByStrategy {

    /**
     * Sorts the records by the designated field and order.
     *
     * @param records   list of JSON record maps
     * @param sortBy    field name to sort by
     * @param sortOrder ASC or DESC
     * @return sorted list of records
     * @throws FieldNotFoundException if sortBy field is not present in any record in the dataset
     */
    public List<Map<String, Object>> sort(
            List<Map<String, Object>> records,
            String sortBy,
            SortOrder sortOrder
    ) {
        if (records == null || records.isEmpty() || sortBy == null || sortBy.isBlank()) {
            return records != null ? new ArrayList<>(records) : new ArrayList<>();
        }

        validateFieldExists(records, sortBy);

        List<Map<String, Object>> sorted = new ArrayList<>(records);
        sorted.sort(new DynamicFieldComparator(sortBy, sortOrder));
        return sorted;
    }

    /**
     * Verifies that the field exists in at least one record of the dataset.
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
