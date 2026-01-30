package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceNumberTest {

    @Test
    void accepts_valid_reference_number() {
        var ref = new ReferenceNumber("ISS-2026-00123");
        assertThat(ref.value()).isEqualTo("ISS-2026-00123");
    }

    @Test
    void normalizes_to_uppercase() {
        var ref = new ReferenceNumber("iss-2026-00123");
        assertThat(ref.value()).isEqualTo("ISS-2026-00123");
    }

    @Test
    void trims_whitespace() {
        var ref = new ReferenceNumber("  ISS-2026-00123  ");
        assertThat(ref.value()).isEqualTo("ISS-2026-00123");
    }

    @Test
    void of_creates_from_components() {
        var ref = ReferenceNumber.of("ISS", 2026, 123);
        assertThat(ref.value()).isEqualTo("ISS-2026-00123");
    }

    @Test
    void of_pads_sequence_with_zeros() {
        var ref = ReferenceNumber.of("ISS", 2026, 1);
        assertThat(ref.value()).isEqualTo("ISS-2026-00001");
    }

    @Test
    void of_handles_max_sequence() {
        var ref = ReferenceNumber.of("ISS", 2026, 99999);
        assertThat(ref.value()).isEqualTo("ISS-2026-99999");
    }

    @Test
    void prefix_extracts_prefix() {
        var ref = new ReferenceNumber("ISS-2026-00123");
        assertThat(ref.prefix()).isEqualTo("ISS");
    }

    @Test
    void year_extracts_year() {
        var ref = new ReferenceNumber("ISS-2026-00123");
        assertThat(ref.year()).isEqualTo(2026);
    }

    @Test
    void sequence_extracts_sequence() {
        var ref = new ReferenceNumber("ISS-2026-00123");
        assertThat(ref.sequence()).isEqualTo(123);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new ReferenceNumber(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Reference number cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new ReferenceNumber(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reference number must be in format XXX-YYYY-NNNNN");
    }

    @Test
    void rejects_wrong_format() {
        assertThatThrownBy(() -> new ReferenceNumber("ISS2026-00123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reference number must be in format XXX-YYYY-NNNNN");
    }

    @Test
    void rejects_short_prefix() {
        assertThatThrownBy(() -> new ReferenceNumber("IS-2026-00123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reference number must be in format XXX-YYYY-NNNNN");
    }

    @Test
    void rejects_short_year() {
        assertThatThrownBy(() -> new ReferenceNumber("ISS-26-00123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reference number must be in format XXX-YYYY-NNNNN");
    }

    @Test
    void rejects_short_sequence() {
        assertThatThrownBy(() -> new ReferenceNumber("ISS-2026-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reference number must be in format XXX-YYYY-NNNNN");
    }

    @Test
    void of_rejects_invalid_prefix_length() {
        assertThatThrownBy(() -> ReferenceNumber.of("IS", 2026, 123))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefix must be exactly 3 characters");
    }

    @Test
    void of_rejects_invalid_year() {
        assertThatThrownBy(() -> ReferenceNumber.of("ISS", 999, 123))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Year must be 4 digits");
    }

    @Test
    void of_rejects_negative_sequence() {
        assertThatThrownBy(() -> ReferenceNumber.of("ISS", 2026, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sequence must be between 0 and 99999");
    }

    @Test
    void of_rejects_sequence_too_large() {
        assertThatThrownBy(() -> ReferenceNumber.of("ISS", 2026, 100000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sequence must be between 0 and 99999");
    }

    @Test
    void toString_returns_value() {
        var ref = new ReferenceNumber("ISS-2026-00123");
        assertThat(ref.toString()).isEqualTo("ISS-2026-00123");
    }

    @Test
    void equals_compares_by_value() {
        var ref1 = new ReferenceNumber("ISS-2026-00123");
        var ref2 = new ReferenceNumber("iss-2026-00123");
        assertThat(ref1).isEqualTo(ref2);
    }
}
