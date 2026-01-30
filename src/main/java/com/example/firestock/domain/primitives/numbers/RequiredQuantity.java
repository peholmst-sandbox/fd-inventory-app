package com.example.firestock.domain.primitives.numbers;

import java.util.Objects;

/**
 * A positive integer representing the required quantity of an item in a manifest entry.
 *
 * <p>Required quantities define how many items of a particular equipment type should
 * be present on an apparatus. For serialised equipment, this is the count of individual
 * items. For consumables, this is the minimum stock level.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be greater than zero (at least 1 item required)</li>
 * </ul>
 *
 * @param value the required quantity, must be positive
 * @throws IllegalArgumentException if the value is zero or negative
 */
public record RequiredQuantity(int value) {
    public RequiredQuantity {
        if (value <= 0) {
            throw new IllegalArgumentException("Required quantity must be positive: " + value);
        }
    }

    /**
     * Creates a required quantity from an integer value.
     *
     * @param value the required quantity (must be positive)
     * @return a new required quantity
     * @throws IllegalArgumentException if the value is zero or negative
     */
    public static RequiredQuantity of(int value) {
        return new RequiredQuantity(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
