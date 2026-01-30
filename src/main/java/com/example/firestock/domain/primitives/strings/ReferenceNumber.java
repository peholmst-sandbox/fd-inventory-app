package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

public record ReferenceNumber(String value) {
    // Format: XXX-YYYY-NNNNN (prefix-year-sequence)
    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}-\\d{4}-\\d{5}$");

    public ReferenceNumber {
        Objects.requireNonNull(value, "Reference number cannot be null");
        value = value.strip().toUpperCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Reference number must be in format XXX-YYYY-NNNNN: " + value);
        }
    }

    public static ReferenceNumber of(String prefix, int year, int sequence) {
        if (prefix == null || prefix.length() != 3) {
            throw new IllegalArgumentException("Prefix must be exactly 3 characters");
        }
        if (year < 1000 || year > 9999) {
            throw new IllegalArgumentException("Year must be 4 digits");
        }
        if (sequence < 0 || sequence > 99999) {
            throw new IllegalArgumentException("Sequence must be between 0 and 99999");
        }
        return new ReferenceNumber(String.format("%s-%04d-%05d", prefix.toUpperCase(), year, sequence));
    }

    public String prefix() {
        return value.substring(0, 3);
    }

    public int year() {
        return Integer.parseInt(value.substring(4, 8));
    }

    public int sequence() {
        return Integer.parseInt(value.substring(9));
    }

    @Override
    public String toString() {
        return value;
    }
}
