package com.assignment.dataset.config;

import com.assignment.dataset.repository.DatasetRecordRepository;
import com.assignment.dataset.service.DatasetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SampleDataSeederTest {

    @Mock
    private DatasetService datasetService;

    @Mock
    private DatasetRecordRepository repository;

    @Mock
    private ApplicationArguments args;

    @Test
    @DisplayName("Should seed 3 sample records when SEED_SAMPLE_DATA is true and dataset is empty")
    void should_seed_when_flag_true_and_dataset_empty() throws Exception {
        when(repository.existsByDatasetName("employee_dataset")).thenReturn(false);

        SampleDataSeeder seeder = new SampleDataSeeder(datasetService, repository, true);
        seeder.run(args);

        verify(datasetService, times(3)).insertRecord(eq("employee_dataset"), anyString());
    }

    @Test
    @DisplayName("Should NOT seed when SEED_SAMPLE_DATA is false")
    void should_not_seed_when_flag_false() throws Exception {
        SampleDataSeeder seeder = new SampleDataSeeder(datasetService, repository, false);
        seeder.run(args);

        verifyNoInteractions(datasetService);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should NOT seed when dataset already contains records even if SEED_SAMPLE_DATA is true")
    void should_not_seed_when_dataset_already_has_records() throws Exception {
        when(repository.existsByDatasetName("employee_dataset")).thenReturn(true);

        SampleDataSeeder seeder = new SampleDataSeeder(datasetService, repository, true);
        seeder.run(args);

        verify(datasetService, never()).insertRecord(anyString(), anyString());
    }
}
