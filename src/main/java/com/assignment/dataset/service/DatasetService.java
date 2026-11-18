package com.assignment.dataset.service;

import com.assignment.dataset.dto.DatasetQueryResponse;
import com.assignment.dataset.dto.RecordCreateResponse;

/**
 * Service interface for dataset operations.
 */
public interface DatasetService {

    /**
     * Validates and inserts a dynamic JSON record into the designated dataset.
     *
     * @param datasetName name of the dataset
     * @param rawJsonBody raw JSON payload string
     * @return confirmation response with database-generated recordId
     */
    RecordCreateResponse insertRecord(String datasetName, String rawJsonBody);

    /**
     * Queries records for a dataset with optional dynamic group-by, sort-by, and sort order.
     *
     * @param datasetName name of the dataset
     * @param groupBy     optional field name to group by
     * @param sortBy      optional field name to sort by
     * @param order       optional sort direction ('asc' or 'desc', defaults to 'asc')
     * @return dataset query response
     */
    DatasetQueryResponse queryDataset(String datasetName, String groupBy, String sortBy, String order);
}
