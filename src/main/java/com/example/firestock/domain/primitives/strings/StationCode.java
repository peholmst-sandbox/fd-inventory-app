package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

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
