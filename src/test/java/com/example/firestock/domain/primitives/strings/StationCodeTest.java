package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationCodeTest {

    @Test
    void accepts_valid_station_code() {
        var code = new StationCode("STA-05");
        assertThat(code.value()).isEqualTo("STA-05");
    }

    @Test
    void normalizes_to_uppercase() {
        var code = new StationCode("sta-05");
        assertThat(code.value()).isEqualTo("STA-05");
    }

    @Test
    void trims_whitespace() {
        var code = new StationCode("  STA-05  ");
        assertThat(code.value()).isEqualTo("STA-05");
    }

    @Test
    void accepts_single_character() {
        var code = new StationCode("A");
        assertThat(code.value()).isEqualTo("A");
    }

    @Test
    void accepts_max_length() {
        var code = new StationCode("A".repeat(20));
        assertThat(code.value()).hasSize(20);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new StationCode(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Station code cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new StationCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station code must be 1-20 characters");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new StationCode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station code must be 1-20 characters");
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new StationCode("A".repeat(21)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station code must be 1-20 characters");
    }

    @Test
    void rejects_invalid_characters() {
        assertThatThrownBy(() -> new StationCode("STA@05"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station code must be alphanumeric with hyphens");
    }

    @Test
    void rejects_spaces() {
        assertThatThrownBy(() -> new StationCode("STA 05"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station code must be alphanumeric with hyphens");
    }

    @Test
    void rejects_starting_with_hyphen() {
        assertThatThrownBy(() -> new StationCode("-STA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station code must be alphanumeric with hyphens");
    }

    @Test
    void toString_returns_value() {
        var code = new StationCode("STA-05");
        assertThat(code.toString()).isEqualTo("STA-05");
    }

    @Test
    void equals_compares_by_value() {
        var code1 = new StationCode("STA-05");
        var code2 = new StationCode("sta-05");
        assertThat(code1).isEqualTo(code2);
    }
}
