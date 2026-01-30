package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A barcode identifier for quick equipment scanning and identification.
 *
 * <p>Barcodes enable rapid equipment lookup during inventory checks using barcode
 * scanners. They may differ from serial numbers and can be department-assigned.
 * Examples include "EQ-SCBA-04523" or "APP-ENG005".
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be 1-100 characters in length</li>
 *   <li>Must start with an alphanumeric character</li>
 *   <li>May contain letters, digits, and hyphens</li>
 *   <li>Leading and trailing whitespace is stripped</li>
 * </ul>
 *
 * @param value the barcode string, must not be null
 * @throws IllegalArgumentException if the value is empty, too long, or contains invalid characters
 */
public record Barcode(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\-]{0,99}$");

    public Barcode {
        Objects.requireNonNull(value, "Barcode cannot be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > 100) {
            throw new IllegalArgumentException("Barcode must be 1-100 characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Barcode must be alphanumeric with hyphens: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
