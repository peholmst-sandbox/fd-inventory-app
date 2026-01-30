package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A contact phone number for users or stations.
 *
 * <p>Phone numbers support international formats and are used for contact purposes.
 * Examples include "+44 161 555 0105" or "(555) 123-4567".
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must be 6-26 characters in length</li>
 *   <li>May optionally start with a + for international format</li>
 *   <li>May contain digits, spaces, hyphens, and parentheses</li>
 *   <li>Must start with +, (, or a digit</li>
 *   <li>Leading and trailing whitespace is stripped</li>
 * </ul>
 *
 * @param value the phone number string, must not be null
 * @throws IllegalArgumentException if the value is empty or has an invalid format
 */
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
