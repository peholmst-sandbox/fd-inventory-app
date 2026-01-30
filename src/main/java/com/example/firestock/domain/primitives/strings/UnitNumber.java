package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An operational identifier for a fire apparatus.
 *
 * <p>Unit numbers are used for quick identification of apparatus during operations
 * and radio communications. Examples include "Engine 5", "Ladder 12", or "Rescue-1".
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be 1-50 characters in length</li>
 *   <li>Must start with an alphanumeric character</li>
 *   <li>May contain letters, digits, spaces, and hyphens</li>
 *   <li>Leading and trailing whitespace is stripped</li>
 * </ul>
 *
 * @param value the unit number string, must not be null
 * @throws IllegalArgumentException if the value is empty, too long, or contains invalid characters
 */
public record UnitNumber(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 \\-]{0,49}$");

    public UnitNumber {
        Objects.requireNonNull(value, "Unit number cannot be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > 50) {
            throw new IllegalArgumentException("Unit number must be 1-50 characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Unit number must be alphanumeric with spaces and hyphens: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
