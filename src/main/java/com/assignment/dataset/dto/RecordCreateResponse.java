package com.assignment.dataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO returned upon successful record insertion.
 *
 * @param message  confirmation message
 * @param dataset  name of the dataset
 * @param recordId database-generated record ID
 */
@Schema(description = "Response returned upon successful record creation")
public record RecordCreateResponse(
        @Schema(description = "Confirmation message", example = "Record added successfully")
        String message,

        @Schema(description = "Target dataset name", example = "employees")
        String dataset,

        @Schema(description = "Database generated record ID", example = "1")
        Long recordId
) {
}
