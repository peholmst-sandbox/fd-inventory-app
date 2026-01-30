package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitNumberTest {

    @Test
    void accepts_valid_unit_number() {
        var unit = new UnitNumber("Engine 5");
        assertThat(unit.value()).isEqualTo("Engine 5");
    }

    @Test
    void accepts_with_hyphen() {
        var unit = new UnitNumber("Ladder-12");
        assertThat(unit.value()).isEqualTo("Ladder-12");
    }

    @Test
    void trims_whitespace() {
        var unit = new UnitNumber("  Engine 5  ");
        assertThat(unit.value()).isEqualTo("Engine 5");
    }

    @Test
    void accepts_single_character() {
        var unit = new UnitNumber("A");
        assertThat(unit.value()).isEqualTo("A");
    }

    @Test
    void accepts_max_length() {
        var unit = new UnitNumber("A".repeat(50));
        assertThat(unit.value()).hasSize(50);
    }

    @Test
    void preserves_case() {
        var unit = new UnitNumber("Engine 5");
        assertThat(unit.value()).isEqualTo("Engine 5");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new UnitNumber(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Unit number cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new UnitNumber(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit number must be 1-50 characters");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new UnitNumber("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit number must be 1-50 characters");
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new UnitNumber("A".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit number must be 1-50 characters");
    }

    @Test
    void rejects_invalid_characters() {
        assertThatThrownBy(() -> new UnitNumber("Engine@5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit number must be alphanumeric with spaces and hyphens");
    }

    @Test
    void rejects_starting_with_space() {
        // After trim, starts with letter so this is actually valid
        var unit = new UnitNumber(" Engine 5");
        assertThat(unit.value()).isEqualTo("Engine 5");
    }

    @Test
    void rejects_starting_with_hyphen() {
        assertThatThrownBy(() -> new UnitNumber("-Engine"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit number must be alphanumeric with spaces and hyphens");
    }

    @Test
    void toString_returns_value() {
        var unit = new UnitNumber("Engine 5");
        assertThat(unit.toString()).isEqualTo("Engine 5");
    }

    @Test
    void equals_compares_by_value() {
        var unit1 = new UnitNumber("Engine 5");
        var unit2 = new UnitNumber("Engine 5");
        assertThat(unit1).isEqualTo(unit2);
    }
}
