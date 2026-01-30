package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated email address for user authentication and communication.
 *
 * <p>Email addresses are used as the primary login identifier for users and for
 * system notifications. The email is automatically normalised to lowercase.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must follow a simplified RFC 5322 email format</li>
 *   <li>Must contain exactly one @ symbol</li>
 *   <li>Must have a valid local part and domain</li>
 *   <li>Automatically converted to lowercase</li>
 *   <li>Leading and trailing whitespace is stripped</li>
 * </ul>
 *
 * @param value the email address string, must not be null
 * @throws IllegalArgumentException if the value is empty or has an invalid format
 */
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
