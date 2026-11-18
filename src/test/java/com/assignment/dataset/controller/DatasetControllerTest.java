package com.assignment.dataset.controller;

import com.assignment.dataset.dto.DatasetQueryResponse;
import com.assignment.dataset.dto.RecordCreateResponse;
import com.assignment.dataset.exception.*;
import com.assignment.dataset.service.DatasetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatasetService datasetService;

    // --- Success Tests ---

    @Test
    @DisplayName("POST /api/dataset/{datasetName}/record should return 201 Created on valid record")
    void should_return_201_when_valid_record_posted() throws Exception {
        when(datasetService.insertRecord(eq("employees"), anyString()))
                .thenReturn(new RecordCreateResponse("Record added successfully", "employees", 1L));

        mockMvc.perform(post("/api/dataset/employees/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"John Doe\",\"age\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("Record added successfully")))
                .andExpect(jsonPath("$.dataset", is("employees")))
                .andExpect(jsonPath("$.recordId", is(1)));
    }

    @Test
    @DisplayName("GET /api/dataset/{datasetName}/query should return 200 with raw records when no params")
    void should_return_200_with_records_when_no_query_params() throws Exception {
        when(datasetService.queryDataset(eq("employees"), isNull(), isNull(), any()))
                .thenReturn(DatasetQueryResponse.ofRecords(List.of(Map.of("name", "Alice"))));

        mockMvc.perform(get("/api/dataset/employees/query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", notNullValue()))
                .andExpect(jsonPath("$.records[0].name", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/dataset/{datasetName}/query should return 200 with sortedRecords when sortBy provided")
    void should_return_200_with_sorted_records() throws Exception {
        when(datasetService.queryDataset(eq("employees"), isNull(), eq("age"), eq("asc")))
                .thenReturn(DatasetQueryResponse.ofSorted(List.of(Map.of("name", "Alice", "age", 25))));

        mockMvc.perform(get("/api/dataset/employees/query")
                        .param("sortBy", "age")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords", notNullValue()))
                .andExpect(jsonPath("$.sortedRecords[0].name", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/dataset/{datasetName}/query should return 200 with groupedRecords when groupBy provided")
    void should_return_200_with_grouped_records() throws Exception {
        when(datasetService.queryDataset(eq("employees"), eq("dept"), isNull(), any()))
                .thenReturn(DatasetQueryResponse.ofGrouped(Map.of("IT", List.of(Map.of("name", "Bob")))));

        mockMvc.perform(get("/api/dataset/employees/query")
                        .param("groupBy", "dept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupedRecords", notNullValue()))
                .andExpect(jsonPath("$.groupedRecords.IT[0].name", is("Bob")));
    }

    // --- Error Case Tests ---

    @Test
    @DisplayName("POST should return 400 when body is empty")
    void should_return_400_when_body_is_empty() throws Exception {
        when(datasetService.insertRecord(eq("employees"), anyString()))
                .thenThrow(new InvalidRecordException("Request body cannot be empty."));

        mockMvc.perform(post("/api/dataset/employees/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/dataset/employees/record")));
    }

    @Test
    @DisplayName("POST should return 400 when body is not a JSON object (array)")
    void should_return_400_when_body_is_not_json_object() throws Exception {
        when(datasetService.insertRecord(eq("employees"), anyString()))
                .thenThrow(new InvalidRecordException("Request body must be a valid JSON object."));

        mockMvc.perform(post("/api/dataset/employees/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"item1\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST should return 400 when body is malformed JSON")
    void should_return_400_when_json_is_malformed() throws Exception {
        when(datasetService.insertRecord(eq("employees"), anyString()))
                .thenThrow(new InvalidRecordException("Malformed JSON payload: unexpected token"));

        mockMvc.perform(post("/api/dataset/employees/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST should return 400 when dataset name has illegal characters")
    void should_return_400_when_dataset_name_is_invalid() throws Exception {
        when(datasetService.insertRecord(eq("invalid@name"), anyString()))
                .thenThrow(new InvalidDatasetNameException("Invalid dataset name"));

        mockMvc.perform(post("/api/dataset/invalid@name/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("GET should return 400 when order parameter is invalid")
    void should_return_400_when_order_param_is_invalid() throws Exception {
        when(datasetService.queryDataset(eq("employees"), any(), any(), eq("invalid_order")))
                .thenThrow(new InvalidQueryParameterException("Invalid order parameter 'invalid_order'. Allowed values are 'asc' or 'desc'."));

        mockMvc.perform(get("/api/dataset/employees/query")
                        .param("order", "invalid_order"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid order parameter 'invalid_order'. Allowed values are 'asc' or 'desc'.")));
    }

    @Test
    @DisplayName("GET should return 400 when groupBy or sortBy field does not exist in dataset")
    void should_return_400_when_field_does_not_exist() throws Exception {
        when(datasetService.queryDataset(eq("employees"), eq("missing_field"), any(), any()))
                .thenThrow(new FieldNotFoundException("Field 'missing_field' does not exist in the dataset."));

        mockMvc.perform(get("/api/dataset/employees/query")
                        .param("groupBy", "missing_field"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Field 'missing_field' does not exist in the dataset.")));
    }

    @Test
    @DisplayName("GET should return 404 when querying dataset that does not exist or has no records")
    void should_return_404_when_dataset_not_found() throws Exception {
        when(datasetService.queryDataset(eq("nonexistent"), any(), any(), any()))
                .thenThrow(new DatasetNotFoundException("Dataset 'nonexistent' not found or contains no records."));

        mockMvc.perform(get("/api/dataset/nonexistent/query"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Dataset 'nonexistent' not found or contains no records.")))
                .andExpect(jsonPath("$.path", is("/api/dataset/nonexistent/query")));
    }

    @Test
    @DisplayName("Should return 500 fallback without leaking stack traces on unexpected exception")
    void should_return_500_fallback_without_leaking_stacktrace() throws Exception {
        when(datasetService.queryDataset(eq("employees"), any(), any(), any()))
                .thenThrow(new RuntimeException("Database connection timeout or internal failure"));

        mockMvc.perform(get("/api/dataset/employees/query"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred. Please contact support.")));
    }
}
