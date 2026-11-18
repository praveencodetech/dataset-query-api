package com.assignment.dataset.config;

import com.assignment.dataset.repository.DatasetRecordRepository;
import com.assignment.dataset.service.DatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup runner that automatically seeds sample records into 'employee_dataset'
 * if SEED_SAMPLE_DATA=true and the dataset is currently empty.
 */
@Component
public class SampleDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);
    public static final String DEMO_DATASET_NAME = "employee_dataset";

    private static final String[] SAMPLE_RECORDS = {
            "{\"id\": 1, \"name\": \"John Doe\", \"age\": 30, \"department\": \"Engineering\"}",
            "{\"id\": 2, \"name\": \"Jane Smith\", \"age\": 25, \"department\": \"Marketing\"}",
            "{\"id\": 3, \"name\": \"Bob Johnson\", \"age\": 35, \"department\": \"Engineering\"}"
    };

    private final DatasetService datasetService;
    private final DatasetRecordRepository repository;
    private final boolean seedSampleData;

    public SampleDataSeeder(
            DatasetService datasetService,
            DatasetRecordRepository repository,
            @Value("${SEED_SAMPLE_DATA:${seed.sample.data:false}}") boolean seedSampleData
    ) {
        this.datasetService = datasetService;
        this.repository = repository;
        this.seedSampleData = seedSampleData;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedSampleData) {
            log.debug("SEED_SAMPLE_DATA is false. Skipping demo dataset seeding.");
            return;
        }

        if (repository.existsByDatasetName(DEMO_DATASET_NAME)) {
            log.info("Dataset '{}' already contains records. Skipping sample data seeding.", DEMO_DATASET_NAME);
            return;
        }

        log.info("SEED_SAMPLE_DATA is true. Seeding demo dataset '{}' with initial records...", DEMO_DATASET_NAME);
        for (String record : SAMPLE_RECORDS) {
            datasetService.insertRecord(DEMO_DATASET_NAME, record);
        }
        log.info("Successfully seeded demo records into dataset '{}'.", DEMO_DATASET_NAME);
    }
}
