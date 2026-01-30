package com.example.firestock.domain.primitives.numbers;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A non-negative quantity value for tracking consumable stock levels.
 *
 * <p>Quantities represent the current amount of a consumable item at a location.
 * They support decimal values with up to 2 decimal places for items measured
 * in fractional units (e.g., litres of foam concentrate).
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must not be null</li>
 *   <li>Must be greater than or equal to zero</li>
 *   <li>Must have at most 2 decimal places</li>
 * </ul>
 *
 * @param value the quantity value, must not be null and must be non-negative
 * @throws IllegalArgumentException if the value is negative or has more than 2 decimal places
 */
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

    /**
     * Creates a quantity from an integer value.
     *
     * @param value the integer quantity
     * @return a new quantity
     */
    public static Quantity of(int value) {
        return new Quantity(BigDecimal.valueOf(value));
    }

    /**
     * Creates a quantity from a string representation.
     *
     * @param value the string representation of the quantity
     * @return a new quantity
     * @throws NumberFormatException if the string is not a valid number
     */
    public static Quantity of(String value) {
        return new Quantity(new BigDecimal(value));
    }

    /**
     * Returns a quantity representing zero.
     *
     * @return a zero quantity
     */
    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    /**
     * Adds another quantity to this one and returns the result.
     *
     * @param other the quantity to add
     * @return a new quantity representing the sum
     */
    public Quantity add(Quantity other) {
        return new Quantity(this.value.add(other.value));
    }

    /**
     * Subtracts another quantity from this one and returns the result.
     *
     * @param other the quantity to subtract
     * @return a new quantity representing the difference
     * @throws IllegalArgumentException if the result would be negative
     */
    public Quantity subtract(Quantity other) {
        return new Quantity(this.value.subtract(other.value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
