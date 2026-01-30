package com.example.firestock.domain.primitives.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void accepts_valid_email() {
        var email = new EmailAddress("john@fire.gov");
        assertThat(email.value()).isEqualTo("john@fire.gov");
    }

    @Test
    void normalizes_to_lowercase() {
        var email = new EmailAddress("John@Fire.Gov");
        assertThat(email.value()).isEqualTo("john@fire.gov");
    }

    @Test
    void trims_whitespace() {
        var email = new EmailAddress("  john@fire.gov  ");
        assertThat(email.value()).isEqualTo("john@fire.gov");
    }

    @Test
    void accepts_subdomain() {
        var email = new EmailAddress("john@mail.fire.gov");
        assertThat(email.value()).isEqualTo("john@mail.fire.gov");
    }

    @Test
    void accepts_plus_sign() {
        var email = new EmailAddress("john+test@fire.gov");
        assertThat(email.value()).isEqualTo("john+test@fire.gov");
    }

    @Test
    void accepts_dots_in_local_part() {
        var email = new EmailAddress("john.doe@fire.gov");
        assertThat(email.value()).isEqualTo("john.doe@fire.gov");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email address cannot be null");
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new EmailAddress(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email address cannot be empty");
    }

    @Test
    void rejects_whitespace_only() {
        assertThatThrownBy(() -> new EmailAddress("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email address cannot be empty");
    }

    @Test
    void rejects_missing_at_sign() {
        assertThatThrownBy(() -> new EmailAddress("johnfire.gov"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    void rejects_missing_domain() {
        assertThatThrownBy(() -> new EmailAddress("john@"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    void rejects_missing_local_part() {
        assertThatThrownBy(() -> new EmailAddress("@fire.gov"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    void rejects_multiple_at_signs() {
        assertThatThrownBy(() -> new EmailAddress("john@@fire.gov"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    void toString_returns_value() {
        var email = new EmailAddress("john@fire.gov");
        assertThat(email.toString()).isEqualTo("john@fire.gov");
    }

    @Test
    void equals_compares_by_value() {
        var email1 = new EmailAddress("john@fire.gov");
        var email2 = new EmailAddress("JOHN@FIRE.GOV");
        assertThat(email1).isEqualTo(email2);
    }
}
