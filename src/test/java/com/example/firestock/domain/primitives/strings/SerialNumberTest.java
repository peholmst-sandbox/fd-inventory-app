package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SerialNumberTest {

    @Test
    void accepts_valid_serial_number() {
        var serial = new SerialNumber("SCT-2020-04523");
        assertThat(serial.value()).isEqualTo("SCT-2020-04523");
    }

    @Test
    void trims_whitespace() {
        var serial = new SerialNumber("  SCT-2020-04523  ");
        assertThat(serial.value()).isEqualTo("SCT-2020-04523");
    }

    @Test
    void accepts_single_character() {
        var serial = new SerialNumber("A");
        assertThat(serial.value()).isEqualTo("A");
    }

    @Test
    void accepts_max_length() {
        var serial = new SerialNumber("A".repeat(100));
        assertThat(serial.value()).hasSize(100);
    }

    @Test
    void preserves_case() {
        var serial = new SerialNumber("Sct-2020-04523");
        assertThat(serial.value()).isEqualTo("Sct-2020-04523");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new SerialNumber(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Serial number cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new SerialNumber(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Serial number must be 1-100 characters");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new SerialNumber("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Serial number must be 1-100 characters");
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new SerialNumber("A".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Serial number must be 1-100 characters");
    }

    @Test
    void rejects_spaces() {
        assertThatThrownBy(() -> new SerialNumber("SCT 2020 04523"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Serial number must be alphanumeric with hyphens");
    }

    @Test
    void rejects_invalid_characters() {
        assertThatThrownBy(() -> new SerialNumber("SCT@2020"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Serial number must be alphanumeric with hyphens");
    }

    @Test
    void rejects_starting_with_hyphen() {
        assertThatThrownBy(() -> new SerialNumber("-SCT2020"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Serial number must be alphanumeric with hyphens");
    }

    @Test
    void toString_returns_value() {
        var serial = new SerialNumber("SCT-2020-04523");
        assertThat(serial.toString()).isEqualTo("SCT-2020-04523");
    }

    @Test
    void equals_compares_by_value() {
        var serial1 = new SerialNumber("SCT-2020-04523");
        var serial2 = new SerialNumber("SCT-2020-04523");
        assertThat(serial1).isEqualTo(serial2);
    }
}
