package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

public record PhoneNumber(String value) {
    // International phone format: optional + followed by digits, spaces, hyphens, and parentheses
    // First character can be + or digit or opening parenthesis
    private static final Pattern PATTERN = Pattern.compile("^\\+?[(]?[0-9][0-9 \\-()]{5,25}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "Phone number cannot be null");
        value = value.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
