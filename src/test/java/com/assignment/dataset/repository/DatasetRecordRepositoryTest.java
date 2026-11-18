package com.assignment.dataset.repository;

import com.assignment.dataset.entity.DatasetRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DatasetRecordRepositoryTest {

    @Autowired
    private DatasetRecordRepository repository;

    @Test
    @DisplayName("Should save a record and retrieve it by dataset name")
    void should_save_and_retrieve_record_by_dataset_name() {
        DatasetRecord record = new DatasetRecord("users", "{\"name\":\"Alice\",\"age\":30}");
        DatasetRecord saved = repository.save(record);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDatasetName()).isEqualTo("users");
        assertThat(saved.getRecordData()).isEqualTo("{\"name\":\"Alice\",\"age\":30}");

        List<DatasetRecord> results = repository.findByDatasetNameOrderByIdAsc("users");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Should check existence by dataset name")
    void should_check_existence_by_dataset_name() {
        repository.save(new DatasetRecord("products", "{\"item\":\"phone\"}"));

        assertThat(repository.existsByDatasetName("products")).isTrue();
        assertThat(repository.existsByDatasetName("non_existent")).isFalse();
    }

    @Test
    @DisplayName("Should retrieve records ordered by ID ascending for dataset")
    void should_retrieve_records_ordered_by_id_asc() {
        repository.save(new DatasetRecord("logs", "{\"msg\":\"first\"}"));
        repository.save(new DatasetRecord("logs", "{\"msg\":\"second\"}"));
        repository.save(new DatasetRecord("other", "{\"msg\":\"ignored\"}"));

        List<DatasetRecord> logs = repository.findByDatasetNameOrderByIdAsc("logs");
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getRecordData()).isEqualTo("{\"msg\":\"first\"}");
        assertThat(logs.get(1).getRecordData()).isEqualTo("{\"msg\":\"second\"}");
    }
}
