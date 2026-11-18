package com.assignment.dataset.strategy;

import com.assignment.dataset.exception.FieldNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupByStrategyTest {

    private GroupByStrategy groupByStrategy;
    private SortByStrategy sortByStrategy;

    @BeforeEach
    void setUp() {
        sortByStrategy = new SortByStrategy();
        groupByStrategy = new GroupByStrategy(sortByStrategy);
    }

    @Test
    @DisplayName("Should group records by specified field")
    void should_group_records_by_field() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "John", "department", "Engineering"),
                Map.of("name", "Jane", "department", "Marketing"),
                Map.of("name", "Bob", "department", "Engineering")
        );

        Map<String, List<Map<String, Object>>> grouped = groupByStrategy.group(records, "department", null, null);

        assertThat(grouped).containsOnlyKeys("Engineering", "Marketing");
        assertThat(grouped.get("Engineering")).hasSize(2)
                .extracting(r -> r.get("name")).containsExactlyInAnyOrder("John", "Bob");
        assertThat(grouped.get("Marketing")).hasSize(1)
                .extracting(r -> r.get("name")).containsExactly("Jane");
    }

    @Test
    @DisplayName("Should group and then sort records inside each group when both groupBy and sortBy are specified")
    void should_group_and_sort_records_when_both_strategies_applied() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "John", "department", "Engineering", "age", 30),
                Map.of("name", "Bob", "department", "Engineering", "age", 25),
                Map.of("name", "Alice", "department", "Engineering", "age", 35),
                Map.of("name", "Jane", "department", "Marketing", "age", 28)
        );

        Map<String, List<Map<String, Object>>> groupedAndSorted = groupByStrategy.group(
                records, "department", "age", SortOrder.ASC
        );

        assertThat(groupedAndSorted.get("Engineering"))
                .extracting(r -> r.get("name"))
                .containsExactly("Bob", "John", "Alice"); // sorted by age: 25, 30, 35
    }

    @Test
    @DisplayName("Should group records with null or missing values into 'null' bucket")
    void should_handle_null_or_missing_group_by_field() {
        Map<String, Object> r1 = Map.of("name", "John", "dept", "IT");
        Map<String, Object> r2 = new HashMap<>();
        r2.put("name", "Jane");
        r2.put("dept", null);
        Map<String, Object> r3 = Map.of("name", "Bob"); // missing "dept"

        List<Map<String, Object>> records = List.of(r1, r2, r3);

        Map<String, List<Map<String, Object>>> grouped = groupByStrategy.group(records, "dept", null, null);

        assertThat(grouped).containsKeys("IT", "null");
        assertThat(grouped.get("null")).hasSize(2)
                .extracting(r -> r.get("name")).containsExactlyInAnyOrder("Jane", "Bob");
    }

    @Test
    @DisplayName("Should throw FieldNotFoundException when groupBy field does not exist in dataset")
    void should_throw_when_group_by_field_does_not_exist_in_dataset() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "John"),
                Map.of("name", "Jane")
        );

        assertThatThrownBy(() -> groupByStrategy.group(records, "nonExistentField", null, null))
                .isInstanceOf(FieldNotFoundException.class)
                .hasMessageContaining("nonExistentField");
    }
}
