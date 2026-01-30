package com.example.firestock.domain.primitives.numbers;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal value) {
    public Quantity {
        Objects.requireNonNull(value, "Quantity cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + value);
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException("Quantity cannot have more than 2 decimal places: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(BigDecimal.valueOf(value));
    }

    public static Quantity of(String value) {
        return new Quantity(new BigDecimal(value));
    }

    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value.add(other.value));
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value.subtract(other.value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
