package com.assignment.dataset.strategy;

import com.assignment.dataset.exception.FieldNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortByStrategyTest {

    private SortByStrategy sortByStrategy;

    @BeforeEach
    void setUp() {
        sortByStrategy = new SortByStrategy();
    }

    @Test
    @DisplayName("Should sort numbers in ascending and descending order")
    void should_sort_numbers_ascending_and_descending() {
        List<Map<String, Object>> records = new ArrayList<>(List.of(
                Map.of("id", 1, "age", 30),
                Map.of("id", 2, "age", 20),
                Map.of("id", 3, "age", 40)
        ));

        List<Map<String, Object>> ascResult = sortByStrategy.sort(records, "age", SortOrder.ASC);
        assertThat(ascResult).extracting(r -> r.get("age"))
                .containsExactly(20, 30, 40);

        List<Map<String, Object>> descResult = sortByStrategy.sort(records, "age", SortOrder.DESC);
        assertThat(descResult).extracting(r -> r.get("age"))
                .containsExactly(40, 30, 20);
    }

    @Test
    @DisplayName("Should sort strings case-insensitively")
    void should_sort_strings_case_insensitive() {
        List<Map<String, Object>> records = new ArrayList<>(List.of(
                Map.of("name", "charlie"),
                Map.of("name", "Alice"),
                Map.of("name", "bob")
        ));

        List<Map<String, Object>> ascResult = sortByStrategy.sort(records, "name", SortOrder.ASC);
        assertThat(ascResult).extracting(r -> r.get("name"))
                .containsExactly("Alice", "bob", "charlie");
    }

    @Test
    @DisplayName("Should sort booleans predictably (false before true in ASC)")
    void should_sort_booleans() {
        List<Map<String, Object>> records = new ArrayList<>(List.of(
                Map.of("active", true),
                Map.of("active", false),
                Map.of("active", true)
        ));

        List<Map<String, Object>> ascResult = sortByStrategy.sort(records, "active", SortOrder.ASC);
        assertThat(ascResult).extracting(r -> r.get("active"))
                .containsExactly(false, true, true);

        List<Map<String, Object>> descResult = sortByStrategy.sort(records, "active", SortOrder.DESC);
        assertThat(descResult).extracting(r -> r.get("active"))
                .containsExactly(true, true, false);
    }

    @Test
    @DisplayName("Should place null or missing values last for both ASC and DESC orders")
    void should_handle_missing_and_null_values_predictably_nulls_last() {
        Map<String, Object> r1 = new HashMap<>();
        r1.put("id", 1);
        r1.put("score", 100);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("id", 2);
        r2.put("score", null);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("id", 3); // missing "score"

        Map<String, Object> r4 = new HashMap<>();
        r4.put("id", 4);
        r4.put("score", 50);

        List<Map<String, Object>> records = List.of(r1, r2, r3, r4);

        List<Map<String, Object>> ascResult = sortByStrategy.sort(records, "score", SortOrder.ASC);
        assertThat(ascResult.get(0).get("id")).isEqualTo(4); // 50
        assertThat(ascResult.get(1).get("id")).isEqualTo(1); // 100
        // null / missing items must be at the end
        assertThat(ascResult.subList(2, 4)).extracting(r -> r.get("score"))
                .containsOnlyNulls();

        List<Map<String, Object>> descResult = sortByStrategy.sort(records, "score", SortOrder.DESC);
        assertThat(descResult.get(0).get("id")).isEqualTo(1); // 100
        assertThat(descResult.get(1).get("id")).isEqualTo(4); // 50
        assertThat(descResult.subList(2, 4)).extracting(r -> r.get("score"))
                .containsOnlyNulls();
    }

    @Test
    @DisplayName("Should handle mixed types safely (number vs string)")
    void should_handle_mixed_types_without_crashing() {
        List<Map<String, Object>> records = List.of(
                Map.of("val", 100),
                Map.of("val", "apple"),
                Map.of("val", 25)
        );

        List<Map<String, Object>> ascResult = sortByStrategy.sort(records, "val", SortOrder.ASC);
        assertThat(ascResult).hasSize(3);
    }

    @Test
    @DisplayName("Should throw FieldNotFoundException when sortBy field does not exist in any record")
    void should_throw_when_field_does_not_exist_in_dataset() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice"),
                Map.of("name", "Bob")
        );

        assertThatThrownBy(() -> sortByStrategy.sort(records, "nonExistentField", SortOrder.ASC))
                .isInstanceOf(FieldNotFoundException.class)
                .hasMessageContaining("nonExistentField");
    }
}
