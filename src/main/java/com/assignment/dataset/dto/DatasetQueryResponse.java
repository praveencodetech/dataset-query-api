package com.assignment.dataset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO returned for dataset query operations.
 * Only the populated query view (records, sortedRecords, or groupedRecords)
 * is serialized in the JSON response.
 *
 * @param records        unsorted, non-grouped dataset records (when no groupBy/sortBy specified)
 * @param sortedRecords  sorted dataset records (when sortBy specified without groupBy)
 * @param groupedRecords grouped dataset records (when groupBy specified, optionally sorted inside each group)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing queried dataset records")
public record DatasetQueryResponse(
        @Schema(description = "Raw records when no groupBy or sortBy is applied")
        List<Map<String, Object>> records,

        @Schema(description = "Records sorted by a specified field")
        List<Map<String, Object>> sortedRecords,

        @Schema(description = "Records grouped by a specified field, with optional sorting within each group")
        Map<String, List<Map<String, Object>>> groupedRecords
) {
        public static DatasetQueryResponse ofRecords(List<Map<String, Object>> records) {
                return new DatasetQueryResponse(records, null, null);
        }

        public static DatasetQueryResponse ofSorted(List<Map<String, Object>> sortedRecords) {
                return new DatasetQueryResponse(null, sortedRecords, null);
        }

        public static DatasetQueryResponse ofGrouped(Map<String, List<Map<String, Object>>> groupedRecords) {
                return new DatasetQueryResponse(null, null, groupedRecords);
        }
}
