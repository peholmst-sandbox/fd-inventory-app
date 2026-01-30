package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {
    // Simplified RFC 5322 pattern for practical email validation
    private static final Pattern PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );

    public EmailAddress {
        Objects.requireNonNull(value, "Email address cannot be null");
        value = value.strip().toLowerCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
