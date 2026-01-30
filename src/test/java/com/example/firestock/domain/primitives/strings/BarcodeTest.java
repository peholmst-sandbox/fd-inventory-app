package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarcodeTest {

    @Test
    void accepts_valid_barcode() {
        var barcode = new Barcode("EQ-SCBA-04523");
        assertThat(barcode.value()).isEqualTo("EQ-SCBA-04523");
    }

    @Test
    void trims_whitespace() {
        var barcode = new Barcode("  EQ-SCBA-04523  ");
        assertThat(barcode.value()).isEqualTo("EQ-SCBA-04523");
    }

    @Test
    void accepts_single_character() {
        var barcode = new Barcode("A");
        assertThat(barcode.value()).isEqualTo("A");
    }

    @Test
    void accepts_max_length() {
        var barcode = new Barcode("A".repeat(100));
        assertThat(barcode.value()).hasSize(100);
    }

    @Test
    void preserves_case() {
        var barcode = new Barcode("Eq-SCBA-04523");
        assertThat(barcode.value()).isEqualTo("Eq-SCBA-04523");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new Barcode(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Barcode cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new Barcode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode must be 1-100 characters");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new Barcode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode must be 1-100 characters");
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new Barcode("A".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode must be 1-100 characters");
    }

    @Test
    void rejects_spaces() {
        assertThatThrownBy(() -> new Barcode("EQ SCBA 04523"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode must be alphanumeric with hyphens");
    }

    @Test
    void rejects_invalid_characters() {
        assertThatThrownBy(() -> new Barcode("EQ@SCBA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode must be alphanumeric with hyphens");
    }

    @Test
    void rejects_starting_with_hyphen() {
        assertThatThrownBy(() -> new Barcode("-EQSCBA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Barcode must be alphanumeric with hyphens");
    }

    @Test
    void toString_returns_value() {
        var barcode = new Barcode("EQ-SCBA-04523");
        assertThat(barcode.toString()).isEqualTo("EQ-SCBA-04523");
    }

    @Test
    void equals_compares_by_value() {
        var barcode1 = new Barcode("EQ-SCBA-04523");
        var barcode2 = new Barcode("EQ-SCBA-04523");
        assertThat(barcode1).isEqualTo(barcode2);
    }
}
