package com.assignment.dataset.controller;

import com.assignment.dataset.dto.DatasetQueryResponse;
import com.assignment.dataset.dto.ErrorResponse;
import com.assignment.dataset.dto.RecordCreateResponse;
import com.assignment.dataset.exception.InvalidRecordException;
import com.assignment.dataset.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller handling HTTP endpoints for dataset record insertion and querying.
 */
@RestController
@RequestMapping("/api/dataset")
@Tag(name = "Dataset API", description = "Operations for storing arbitrary JSON records and executing dynamic group/sort queries")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /**
     * Inserts an arbitrary JSON record into the designated dataset.
     */
    @Operation(
            summary = "Insert a record into a dataset",
            description = "Accepts any valid, non-empty JSON object and persists it to the specified dataset."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Record added successfully",
                    content = @Content(schema = @Schema(implementation = RecordCreateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request (empty body, malformed JSON, not a JSON object, or invalid dataset name)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(
            value = "/{datasetName}/record",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RecordCreateResponse> insertRecord(
            @Parameter(description = "Dataset identifier", example = "employees")
            @PathVariable("datasetName") String datasetName,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Any valid non-empty JSON object",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "SampleEmployee",
                                    value = "{\"id\": 1, \"name\": \"John Doe\", \"age\": 30, \"department\": \"Engineering\"}"
                            )
                    )
            )
            @RequestBody(required = false) String rawBody
    ) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new InvalidRecordException("Request body cannot be empty.");
        }
        RecordCreateResponse response = datasetService.insertRecord(datasetName, rawBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Queries records from a dataset with dynamic grouping and sorting.
     */
    @Operation(
            summary = "Query records from a dataset",
            description = "Retrieves records from a dataset. Supports optional groupBy, sortBy, and order parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dataset queried successfully",
                    content = @Content(schema = @Schema(implementation = DatasetQueryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request (invalid order parameter, or requested field does not exist)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Dataset not found or has no records",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(
            value = "/{datasetName}/query",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DatasetQueryResponse> queryRecords(
            @Parameter(description = "Dataset identifier", example = "employees")
            @PathVariable("datasetName") String datasetName,

            @Parameter(description = "Optional field name to group records by", example = "department")
            @RequestParam(name = "groupBy", required = false) String groupBy,

            @Parameter(description = "Optional field name to sort records by", example = "age")
            @RequestParam(name = "sortBy", required = false) String sortBy,

            @Parameter(description = "Sort direction: 'asc' or 'desc' (defaults to 'asc')", example = "asc")
            @RequestParam(name = "order", required = false, defaultValue = "asc") String order
    ) {
        DatasetQueryResponse response = datasetService.queryDataset(datasetName, groupBy, sortBy, order);
        return ResponseEntity.ok(response);
    }
}
