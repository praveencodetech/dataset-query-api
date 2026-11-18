package com.assignment.dataset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standardized API error response body.
 *
 * @param timestamp time when the error occurred
 * @param status    HTTP status code (e.g. 400, 404, 500)
 * @param error     HTTP status reason phrase (e.g. "Bad Request", "Not Found")
 * @param message   human-readable details describing the error
 * @param path      request URI that generated this error
 */
@Schema(description = "Standardized error response payload")
public record ErrorResponse(
        @Schema(description = "Timestamp of error occurrence", example = "2026-11-18T06:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP error title", example = "Bad Request")
        String error,

        @Schema(description = "Detailed error description", example = "Field 'age' does not exist in dataset.")
        String message,

        @Schema(description = "Target API request path", example = "/api/dataset/employees/query")
        String path
) {
        public static ErrorResponse of(int status, String error, String message, String path) {
                return new ErrorResponse(Instant.now(), status, error, message, path);
        }
}
