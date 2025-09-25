package com.luckxpress.data.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    @DisplayName("convertToDatabaseColumn: null -> []")
    void convertToDatabaseColumn_null_returnsEmptyArrayJson() {
        String json = converter.convertToDatabaseColumn(null);
        assertThat(json).isEqualTo("[]");
    }

    @Test
    @DisplayName("convertToDatabaseColumn: empty list -> []")
    void convertToDatabaseColumn_empty_returnsEmptyArrayJson() {
        String json = converter.convertToDatabaseColumn(new ArrayList<>());
        assertThat(json).isEqualTo("[]");
    }

    @Test
    @DisplayName("convertToDatabaseColumn: normal list -> JSON array")
    void convertToDatabaseColumn_normal_returnsJsonArray() {
        List<String> roles = List.of("ADMIN", "USER");
        String json = converter.convertToDatabaseColumn(roles);

        // Parse back using the same converter path to avoid brittle string equality
        List<String> roundTrip = converter.convertToEntityAttribute(json);
        assertThat(roundTrip).containsExactlyElementsOf(roles);
    }

    @Test
    @DisplayName("convertToEntityAttribute: null/empty -> empty list")
    void convertToEntityAttribute_nullOrEmpty_returnsEmptyList() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute(" ")).isEmpty();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    @DisplayName("convertToEntityAttribute: valid JSON -> list of strings")
    void convertToEntityAttribute_validJson_parsesList() {
        String json = "[\"A\",\"B\",\"C\"]";
        List<String> list = converter.convertToEntityAttribute(json);
        assertThat(list).containsExactly("A", "B", "C");
    }

    @Test
    @DisplayName("convertToEntityAttribute: invalid JSON -> empty list and logged error")
    void convertToEntityAttribute_invalidJson_returnsEmptyList() {
        String badJson = "not-a-json";
        List<String> list = converter.convertToEntityAttribute(badJson);
        assertThat(list).isEmpty();
    }
}
