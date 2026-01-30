package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BadgeNumberTest {

    @Test
    void accepts_valid_badge_number() {
        var badge = new BadgeNumber("FD-1234");
        assertThat(badge.value()).isEqualTo("FD-1234");
    }

    @Test
    void accepts_alphanumeric() {
        var badge = new BadgeNumber("B2025001");
        assertThat(badge.value()).isEqualTo("B2025001");
    }

    @Test
    void trims_whitespace() {
        var badge = new BadgeNumber("  FD-1234  ");
        assertThat(badge.value()).isEqualTo("FD-1234");
    }

    @Test
    void accepts_single_character() {
        var badge = new BadgeNumber("A");
        assertThat(badge.value()).isEqualTo("A");
    }

    @Test
    void accepts_max_length() {
        var badge = new BadgeNumber("A".repeat(50));
        assertThat(badge.value()).hasSize(50);
    }

    @Test
    void preserves_case() {
        var badge = new BadgeNumber("Fd-1234");
        assertThat(badge.value()).isEqualTo("Fd-1234");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new BadgeNumber(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Badge number cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new BadgeNumber(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge number must be 1-50 characters");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new BadgeNumber("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge number must be 1-50 characters");
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new BadgeNumber("A".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge number must be 1-50 characters");
    }

    @Test
    void rejects_spaces() {
        assertThatThrownBy(() -> new BadgeNumber("FD 1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge number must be alphanumeric with hyphens");
    }

    @Test
    void rejects_invalid_characters() {
        assertThatThrownBy(() -> new BadgeNumber("FD@1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge number must be alphanumeric with hyphens");
    }

    @Test
    void rejects_starting_with_hyphen() {
        assertThatThrownBy(() -> new BadgeNumber("-FD1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge number must be alphanumeric with hyphens");
    }

    @Test
    void toString_returns_value() {
        var badge = new BadgeNumber("FD-1234");
        assertThat(badge.toString()).isEqualTo("FD-1234");
    }

    @Test
    void equals_compares_by_value() {
        var badge1 = new BadgeNumber("FD-1234");
        var badge2 = new BadgeNumber("FD-1234");
        assertThat(badge1).isEqualTo(badge2);
    }
}
