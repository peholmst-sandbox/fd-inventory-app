package com.example.firestock.domain.primitives.numbers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequiredQuantityTest {

    @Test
    void accepts_positive_value() {
        var qty = new RequiredQuantity(5);
        assertThat(qty.value()).isEqualTo(5);
    }

    @Test
    void accepts_one() {
        var qty = new RequiredQuantity(1);
        assertThat(qty.value()).isEqualTo(1);
    }

    @Test
    void accepts_large_value() {
        var qty = new RequiredQuantity(Integer.MAX_VALUE);
        assertThat(qty.value()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void of_creates_quantity() {
        var qty = RequiredQuantity.of(10);
        assertThat(qty.value()).isEqualTo(10);
    }

    @Test
    void rejects_zero() {
        assertThatThrownBy(() -> new RequiredQuantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required quantity must be positive");
    }

    @Test
    void rejects_negative() {
        assertThatThrownBy(() -> new RequiredQuantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required quantity must be positive");
    }

    @Test
    void toString_returns_value_string() {
        var qty = new RequiredQuantity(5);
        assertThat(qty.toString()).isEqualTo("5");
    }

    @Test
    void equals_compares_by_value() {
        var qty1 = new RequiredQuantity(5);
        var qty2 = new RequiredQuantity(5);
        assertThat(qty1).isEqualTo(qty2);
    }
}
