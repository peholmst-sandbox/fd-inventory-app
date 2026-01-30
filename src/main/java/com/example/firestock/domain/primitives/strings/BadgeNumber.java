package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

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
