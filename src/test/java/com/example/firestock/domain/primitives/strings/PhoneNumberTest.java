package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void accepts_international_format() {
        var phone = new PhoneNumber("+44 161 555 0105");
        assertThat(phone.value()).isEqualTo("+44 161 555 0105");
    }

    @Test
    void accepts_us_format() {
        var phone = new PhoneNumber("(555) 123-4567");
        assertThat(phone.value()).isEqualTo("(555) 123-4567");
    }

    @Test
    void accepts_simple_format() {
        var phone = new PhoneNumber("5551234567");
        assertThat(phone.value()).isEqualTo("5551234567");
    }

    @Test
    void accepts_dashes() {
        var phone = new PhoneNumber("555-123-4567");
        assertThat(phone.value()).isEqualTo("555-123-4567");
    }

    @Test
    void trims_whitespace() {
        var phone = new PhoneNumber("  555-123-4567  ");
        assertThat(phone.value()).isEqualTo("555-123-4567");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Phone number cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new PhoneNumber(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number cannot be empty");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new PhoneNumber("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number cannot be empty");
    }

    @Test
    void rejects_too_short() {
        assertThatThrownBy(() -> new PhoneNumber("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void rejects_letters() {
        assertThatThrownBy(() -> new PhoneNumber("555-ABC-1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void rejects_starting_with_special_char() {
        assertThatThrownBy(() -> new PhoneNumber("-5551234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void toString_returns_value() {
        var phone = new PhoneNumber("+44 161 555 0105");
        assertThat(phone.toString()).isEqualTo("+44 161 555 0105");
    }

    @Test
    void equals_compares_by_value() {
        var phone1 = new PhoneNumber("555-123-4567");
        var phone2 = new PhoneNumber("555-123-4567");
        assertThat(phone1).isEqualTo(phone2);
    }
}
