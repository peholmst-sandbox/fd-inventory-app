package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A short identifier code for a fire station.
 *
 * <p>Station codes are used for quick identification of stations and must be unique
 * within the system. Examples include "STA-05" or "FD-NORTH".
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be 1-20 characters in length</li>
 *   <li>Must start with an alphanumeric character</li>
 *   <li>May contain uppercase letters, digits, and hyphens</li>
 *   <li>Automatically converted to uppercase</li>
 * </ul>
 *
 * @param value the station code string, must not be null
 * @throws IllegalArgumentException if the value is empty, too long, or contains invalid characters
 */
public record StationCode(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9\\-]{0,19}$");

    public StationCode {
        Objects.requireNonNull(value, "Station code cannot be null");
        value = value.strip().toUpperCase();
        if (value.isEmpty() || value.length() > 20) {
            throw new IllegalArgumentException("Station code must be 1-20 characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Station code must be alphanumeric with hyphens: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
