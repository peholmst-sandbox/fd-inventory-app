package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

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
