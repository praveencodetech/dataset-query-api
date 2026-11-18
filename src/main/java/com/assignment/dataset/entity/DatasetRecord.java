package com.assignment.dataset.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a single stored record in a dynamic dataset.
 */
@Entity
@Table(
        name = "dataset_record",
        indexes = {
                @Index(name = "idx_dataset_name", columnList = "dataset_name")
        }
)
public class DatasetRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_name", nullable = false)
    private String datasetName;

    @Lob
    @Column(name = "record_data", columnDefinition = "TEXT", nullable = false)
    private String recordData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Default constructor for JPA.
     */
    public DatasetRecord() {
    }

    /**
     * Constructor for creating a new dataset record.
     *
     * @param datasetName name of the dataset
     * @param recordData  raw JSON string
     */
    public DatasetRecord(String datasetName, String recordData) {
        this.datasetName = datasetName;
        this.recordData = recordData;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getRecordData() {
        return recordData;
    }

    public void setRecordData(String recordData) {
        this.recordData = recordData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatasetRecord that = (DatasetRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
