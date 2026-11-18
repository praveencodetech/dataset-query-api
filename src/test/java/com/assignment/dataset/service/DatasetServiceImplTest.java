package com.assignment.dataset.service;

import com.assignment.dataset.dto.DatasetQueryResponse;
import com.assignment.dataset.dto.RecordCreateResponse;
import com.assignment.dataset.entity.DatasetRecord;
import com.assignment.dataset.exception.DatasetNotFoundException;
import com.assignment.dataset.exception.FieldNotFoundException;
import com.assignment.dataset.exception.InvalidDatasetNameException;
import com.assignment.dataset.exception.InvalidRecordException;
import com.assignment.dataset.repository.DatasetRecordRepository;
import com.assignment.dataset.strategy.GroupByStrategy;
import com.assignment.dataset.strategy.SortByStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatasetServiceImplTest {

    @Mock
    private DatasetRecordRepository repository;

    private ObjectMapper objectMapper;
    private SortByStrategy sortByStrategy;
    private GroupByStrategy groupByStrategy;
    private DatasetService datasetService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sortByStrategy = new SortByStrategy();
        groupByStrategy = new GroupByStrategy(sortByStrategy);
        datasetService = new DatasetServiceImpl(repository, objectMapper, sortByStrategy, groupByStrategy);
    }

    @Test
    @DisplayName("Should insert valid JSON object and return 201 response DTO")
    void should_insert_record_successfully_when_valid_json_object() {
        String json = "{\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}";
        DatasetRecord saved = new DatasetRecord("employees", json);
        saved.setId(101L);

        when(repository.save(any(DatasetRecord.class))).thenReturn(saved);

        RecordCreateResponse response = datasetService.insertRecord("employees", json);

        assertThat(response.dataset()).isEqualTo("employees");
        assertThat(response.recordId()).isEqualTo(101L);
        assertThat(response.message()).isEqualTo("Record added successfully");
        verify(repository, times(1)).save(any(DatasetRecord.class));
    }

    @Test
    @DisplayName("Should preserve internal 'id' attribute inside the stored JSON")
    void should_preserve_internal_id_in_stored_json_when_inserting() {
        String json = "{\"id\":999,\"name\":\"John Doe\"}";
        DatasetRecord saved = new DatasetRecord("employees", json);
        saved.setId(1L);

        when(repository.save(any(DatasetRecord.class))).thenReturn(saved);

        RecordCreateResponse response = datasetService.insertRecord("employees", json);

        assertThat(response.recordId()).isEqualTo(1L); // Database generated ID
        // Verify repository received unchanged payload
        verify(repository).save(argThat(r -> r.getRecordData().contains("\"id\":999")));
    }

    @Test
    @DisplayName("Should throw InvalidRecordException when payload is empty or blank")
    void should_throw_when_inserting_empty_or_blank_record() {
        assertThatThrownBy(() -> datasetService.insertRecord("employees", ""))
                .isInstanceOf(InvalidRecordException.class);

        assertThatThrownBy(() -> datasetService.insertRecord("employees", "   "))
                .isInstanceOf(InvalidRecordException.class);

        assertThatThrownBy(() -> datasetService.insertRecord("employees", "{}"))
                .isInstanceOf(InvalidRecordException.class);
    }

    @Test
    @DisplayName("Should throw InvalidRecordException when payload is not a JSON object")
    void should_throw_when_inserting_non_object_json() {
        assertThatThrownBy(() -> datasetService.insertRecord("employees", "[\"item1\", \"item2\"]"))
                .isInstanceOf(InvalidRecordException.class);

        assertThatThrownBy(() -> datasetService.insertRecord("employees", "\"string value\""))
                .isInstanceOf(InvalidRecordException.class);

        assertThatThrownBy(() -> datasetService.insertRecord("employees", "12345"))
                .isInstanceOf(InvalidRecordException.class);
    }

    @Test
    @DisplayName("Should throw InvalidDatasetNameException when dataset name has illegal characters or is blank")
    void should_throw_when_inserting_with_invalid_dataset_name() {
        String json = "{\"a\":1}";
        assertThatThrownBy(() -> datasetService.insertRecord("   ", json))
                .isInstanceOf(InvalidDatasetNameException.class);

        assertThatThrownBy(() -> datasetService.insertRecord("invalid/dataset/name", json))
                .isInstanceOf(InvalidDatasetNameException.class);

        assertThatThrownBy(() -> datasetService.insertRecord("name with spaces", json))
                .isInstanceOf(InvalidDatasetNameException.class);
    }

    @Test
    @DisplayName("Should return records in insertion order when neither groupBy nor sortBy is requested")
    void should_query_records_without_params() {
        DatasetRecord r1 = new DatasetRecord("employees", "{\"name\":\"Alice\",\"age\":30}");
        DatasetRecord r2 = new DatasetRecord("employees", "{\"name\":\"Bob\",\"age\":25}");
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(r1, r2));

        DatasetQueryResponse response = datasetService.queryDataset("employees", null, null, null);

        assertThat(response.records()).hasSize(2);
        assertThat(response.sortedRecords()).isNull();
        assertThat(response.groupedRecords()).isNull();
        assertThat(response.records().get(0)).containsEntry("name", "Alice");
        assertThat(response.records().get(1)).containsEntry("name", "Bob");
    }

    @Test
    @DisplayName("Should return sortedRecords when sortBy is requested")
    void should_query_with_sort_by() {
        DatasetRecord r1 = new DatasetRecord("employees", "{\"name\":\"Alice\",\"age\":30}");
        DatasetRecord r2 = new DatasetRecord("employees", "{\"name\":\"Bob\",\"age\":25}");
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(r1, r2));

        DatasetQueryResponse response = datasetService.queryDataset("employees", null, "age", "asc");

        assertThat(response.sortedRecords()).hasSize(2);
        assertThat(response.records()).isNull();
        assertThat(response.groupedRecords()).isNull();
        assertThat(response.sortedRecords().get(0)).containsEntry("name", "Bob");
        assertThat(response.sortedRecords().get(1)).containsEntry("name", "Alice");
    }

    @Test
    @DisplayName("Should return groupedRecords when groupBy is requested")
    void should_query_with_group_by() {
        DatasetRecord r1 = new DatasetRecord("employees", "{\"name\":\"Alice\",\"department\":\"HR\"}");
        DatasetRecord r2 = new DatasetRecord("employees", "{\"name\":\"Bob\",\"department\":\"IT\"}");
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(r1, r2));

        DatasetQueryResponse response = datasetService.queryDataset("employees", "department", null, null);

        assertThat(response.groupedRecords()).isNotNull();
        assertThat(response.records()).isNull();
        assertThat(response.sortedRecords()).isNull();
        assertThat(response.groupedRecords()).containsKeys("HR", "IT");
    }

    @Test
    @DisplayName("Should group first then sort within each group when both groupBy and sortBy are requested")
    void should_query_with_both_group_by_and_sort_by() {
        DatasetRecord r1 = new DatasetRecord("employees", "{\"name\":\"John\",\"dept\":\"IT\",\"age\":30}");
        DatasetRecord r2 = new DatasetRecord("employees", "{\"name\":\"Bob\",\"dept\":\"IT\",\"age\":22}");
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(r1, r2));

        DatasetQueryResponse response = datasetService.queryDataset("employees", "dept", "age", "asc");

        assertThat(response.groupedRecords()).isNotNull();
        List<Map<String, Object>> itGroup = response.groupedRecords().get("IT");
        assertThat(itGroup).hasSize(2);
        assertThat(itGroup.get(0)).containsEntry("name", "Bob"); // age 22
        assertThat(itGroup.get(1)).containsEntry("name", "John"); // age 30
    }

    @Test
    @DisplayName("Should throw DatasetNotFoundException (404) when dataset has no records")
    void should_throw_404_when_querying_non_existent_or_empty_dataset() {
        when(repository.findByDatasetNameOrderByIdAsc("unknown")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> datasetService.queryDataset("unknown", null, null, null))
                .isInstanceOf(DatasetNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
