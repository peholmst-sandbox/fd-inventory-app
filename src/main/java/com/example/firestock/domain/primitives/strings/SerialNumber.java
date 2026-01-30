package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A manufacturer or assigned serial number for tracking individual equipment items.
 *
 * <p>Serial numbers uniquely identify individual pieces of equipment and are used
 * for tracking, maintenance records, and warranty purposes. Examples include
 * "SCT-2020-04523" or "ABC123456".
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be 1-100 characters in length</li>
 *   <li>Must start with an alphanumeric character</li>
 *   <li>May contain letters, digits, and hyphens</li>
 *   <li>Leading and trailing whitespace is stripped</li>
 * </ul>
 *
 * @param value the serial number string, must not be null
 * @throws IllegalArgumentException if the value is empty, too long, or contains invalid characters
 */
public record SerialNumber(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\-]{0,99}$");

    public SerialNumber {
        Objects.requireNonNull(value, "Serial number cannot be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > 100) {
            throw new IllegalArgumentException("Serial number must be 1-100 characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Serial number must be alphanumeric with hyphens: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
