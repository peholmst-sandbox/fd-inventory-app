package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An employee or badge identifier for a firefighter or staff member.
 *
 * <p>Badge numbers are used for personnel identification and may be printed on
 * physical badges. Examples include "FD-1234" or "FF12345".
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be 1-50 characters in length</li>
 *   <li>Must start with an alphanumeric character</li>
 *   <li>May contain letters, digits, and hyphens</li>
 *   <li>Leading and trailing whitespace is stripped</li>
 * </ul>
 *
 * @param value the badge number string, must not be null
 * @throws IllegalArgumentException if the value is empty, too long, or contains invalid characters
 */
public record BadgeNumber(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\-]{0,49}$");

    public BadgeNumber {
        Objects.requireNonNull(value, "Badge number cannot be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > 50) {
            throw new IllegalArgumentException("Badge number must be 1-50 characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Badge number must be alphanumeric with hyphens: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
