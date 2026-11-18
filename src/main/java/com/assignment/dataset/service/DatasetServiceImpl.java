package com.assignment.dataset.service;

import com.assignment.dataset.dto.DatasetQueryResponse;
import com.assignment.dataset.dto.RecordCreateResponse;
import com.assignment.dataset.entity.DatasetRecord;
import com.assignment.dataset.exception.DatasetNotFoundException;
import com.assignment.dataset.exception.InvalidDatasetNameException;
import com.assignment.dataset.exception.InvalidRecordException;
import com.assignment.dataset.repository.DatasetRecordRepository;
import com.assignment.dataset.strategy.GroupByStrategy;
import com.assignment.dataset.strategy.SortByStrategy;
import com.assignment.dataset.strategy.SortOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Production implementation of {@link DatasetService}.
 * Handles dataset record validation, persistence, and dynamic querying.
 */
@Service
public class DatasetServiceImpl implements DatasetService {

    private static final Logger log = LoggerFactory.getLogger(DatasetServiceImpl.class);
    private static final Pattern DATASET_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};
    private static final String RECORD_SUCCESS_MESSAGE = "Record added successfully";

    private final DatasetRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final SortByStrategy sortByStrategy;
    private final GroupByStrategy groupByStrategy;

    public DatasetServiceImpl(
            DatasetRecordRepository repository,
            ObjectMapper objectMapper,
            SortByStrategy sortByStrategy,
            GroupByStrategy groupByStrategy
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.sortByStrategy = sortByStrategy;
        this.groupByStrategy = groupByStrategy;
    }

    @Override
    @Transactional
    public RecordCreateResponse insertRecord(String datasetName, String rawJsonBody) {
        validateDatasetName(datasetName);
        validateRecordJson(rawJsonBody);

        log.debug("Inserting new record for dataset: {}", datasetName);
        DatasetRecord entity = new DatasetRecord(datasetName, rawJsonBody);
        DatasetRecord saved = repository.save(entity);

        log.info("Record inserted successfully for dataset '{}' with recordId {}", datasetName, saved.getId());
        return new RecordCreateResponse(RECORD_SUCCESS_MESSAGE, datasetName, saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetQueryResponse queryDataset(String datasetName, String groupBy, String sortBy, String order) {
        validateDatasetName(datasetName);

        List<DatasetRecord> records = repository.findByDatasetNameOrderByIdAsc(datasetName);
        if (records.isEmpty()) {
            throw new DatasetNotFoundException(
                    "Dataset '" + datasetName + "' not found or contains no records."
            );
        }

        List<Map<String, Object>> parsedRecords = deserializeRecords(records);
        SortOrder sortOrder = SortOrder.fromString(order);

        boolean hasGroupBy = groupBy != null && !groupBy.isBlank();
        boolean hasSortBy = sortBy != null && !sortBy.isBlank();

        if (hasGroupBy) {
            log.debug("Executing group-by query on dataset '{}' with groupBy='{}', sortBy='{}', order='{}'",
                    datasetName, groupBy, sortBy, sortOrder);
            Map<String, List<Map<String, Object>>> grouped = groupByStrategy.group(
                    parsedRecords, groupBy.trim(), hasSortBy ? sortBy.trim() : null, sortOrder
            );
            return DatasetQueryResponse.ofGrouped(grouped);
        }

        if (hasSortBy) {
            log.debug("Executing sort-by query on dataset '{}' with sortBy='{}', order='{}'",
                    datasetName, sortBy, sortOrder);
            List<Map<String, Object>> sorted = sortByStrategy.sort(
                    parsedRecords, sortBy.trim(), sortOrder
            );
            return DatasetQueryResponse.ofSorted(sorted);
        }

        log.debug("Returning all records in insertion order for dataset '{}'", datasetName);
        return DatasetQueryResponse.ofRecords(parsedRecords);
    }

    private void validateDatasetName(String datasetName) {
        if (datasetName == null || datasetName.isBlank() || !DATASET_NAME_PATTERN.matcher(datasetName).matches()) {
            throw new InvalidDatasetNameException(
                    "Invalid dataset name '" + datasetName + "'. Names must be non-blank and contain only alphanumeric characters, underscores, or hyphens."
            );
        }
    }

    private void validateRecordJson(String rawJsonBody) {
        if (rawJsonBody == null || rawJsonBody.isBlank()) {
            throw new InvalidRecordException("Request body cannot be empty.");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(rawJsonBody);
            if (!rootNode.isObject()) {
                throw new InvalidRecordException("Request body must be a valid JSON object.");
            }
            if (rootNode.isEmpty()) {
                throw new InvalidRecordException("Request body must be a non-empty JSON object.");
            }
        } catch (JsonProcessingException e) {
            throw new InvalidRecordException("Malformed JSON payload: " + e.getOriginalMessage(), e);
        }
    }

    private List<Map<String, Object>> deserializeRecords(List<DatasetRecord> records) {
        List<Map<String, Object>> result = new ArrayList<>(records.size());
        for (DatasetRecord record : records) {
            try {
                Map<String, Object> map = objectMapper.readValue(record.getRecordData(), MAP_TYPE_REF);
                result.add(map);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize record ID {} for dataset '{}'", record.getId(), record.getDatasetName(), e);
                throw new IllegalStateException("Corrupt record stored in database for record ID: " + record.getId(), e);
            }
        }
        return result;
    }
}
