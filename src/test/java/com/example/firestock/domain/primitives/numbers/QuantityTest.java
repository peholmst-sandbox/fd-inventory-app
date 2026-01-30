package com.example.firestock.domain.primitives.numbers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void accepts_zero() {
        var qty = new Quantity(BigDecimal.ZERO);
        assertThat(qty.value()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void accepts_positive_integer() {
        var qty = new Quantity(new BigDecimal("10"));
        assertThat(qty.value()).isEqualTo(new BigDecimal("10"));
    }

    @Test
    void accepts_positive_decimal() {
        var qty = new Quantity(new BigDecimal("10.50"));
        assertThat(qty.value()).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void accepts_two_decimal_places() {
        var qty = new Quantity(new BigDecimal("10.99"));
        assertThat(qty.value()).isEqualTo(new BigDecimal("10.99"));
    }

    @Test
    void of_int_creates_quantity() {
        var qty = Quantity.of(10);
        assertThat(qty.value()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void of_string_creates_quantity() {
        var qty = Quantity.of("10.50");
        assertThat(qty.value()).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void zero_returns_zero_quantity() {
        var qty = Quantity.zero();
        assertThat(qty.value()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void add_sums_quantities() {
        var qty1 = Quantity.of(10);
        var qty2 = Quantity.of(5);
        var result = qty1.add(qty2);
        assertThat(result.value()).isEqualTo(new BigDecimal("15"));
    }

    @Test
    void subtract_differences_quantities() {
        var qty1 = Quantity.of(10);
        var qty2 = Quantity.of(3);
        var result = qty1.subtract(qty2);
        assertThat(result.value()).isEqualTo(new BigDecimal("7"));
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new Quantity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Quantity cannot be null");
    }

    @Test
    void rejects_negative() {
        assertThatThrownBy(() -> new Quantity(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    void rejects_more_than_two_decimal_places() {
        assertThatThrownBy(() -> new Quantity(new BigDecimal("10.123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot have more than 2 decimal places");
    }

    @Test
    void subtract_rejects_negative_result() {
        var qty1 = Quantity.of(3);
        var qty2 = Quantity.of(10);
        assertThatThrownBy(() -> qty1.subtract(qty2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    void toString_returns_value_string() {
        var qty = Quantity.of("10.50");
        assertThat(qty.toString()).isEqualTo("10.50");
    }

    @Test
    void equals_compares_by_value() {
        var qty1 = Quantity.of(10);
        var qty2 = Quantity.of(10);
        assertThat(qty1).isEqualTo(qty2);
    }
}
