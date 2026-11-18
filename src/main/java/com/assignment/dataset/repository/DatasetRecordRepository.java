package com.assignment.dataset.repository;

import com.assignment.dataset.entity.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for {@link DatasetRecord}.
 */
@Repository
public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, Long> {

    /**
     * Checks if any records exist for the given dataset name.
     *
     * @param datasetName name of the dataset
     * @return true if at least one record exists, false otherwise
     */
    boolean existsByDatasetName(String datasetName);

    /**
     * Finds all records belonging to a dataset ordered by ID ascending.
     *
     * @param datasetName name of the dataset
     * @return list of records in insertion order
     */
    List<DatasetRecord> findByDatasetNameOrderByIdAsc(String datasetName);
}
