package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

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
