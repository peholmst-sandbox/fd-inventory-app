package com.example.firestock.domain.primitives.numbers;

import java.util.Objects;

public record RequiredQuantity(int value) {
    public RequiredQuantity {
        if (value <= 0) {
            throw new IllegalArgumentException("Required quantity must be positive: " + value);
        }
    }

    public static RequiredQuantity of(int value) {
        return new RequiredQuantity(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
