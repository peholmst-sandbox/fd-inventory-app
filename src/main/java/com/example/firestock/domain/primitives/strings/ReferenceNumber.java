package com.example.firestock.domain.primitives.strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A human-readable reference number for issues, transfers, and restock records.
 *
 * <p>Reference numbers follow a structured format that includes a prefix, year, and
 * sequence number for easy identification and communication. Examples include
 * "ISS-2026-00123" for issues or "TRF-2026-00456" for transfers.
 *
 * <p>Format: {@code XXX-YYYY-NNNNN} where:
 * <ul>
 *   <li>XXX - 3-letter prefix (e.g., ISS, TRF, RST)</li>
 *   <li>YYYY - 4-digit year</li>
 *   <li>NNNNN - 5-digit sequence number (zero-padded)</li>
 * </ul>
 *
 * <p>Automatically converted to uppercase.
 *
 * @param value the reference number string, must not be null
 * @throws IllegalArgumentException if the value does not match the required format
 */
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

    /**
     * Creates a reference number from individual components.
     *
     * @param prefix   the 3-character prefix (e.g., "ISS", "TRF", "RST")
     * @param year     the 4-digit year (1000-9999)
     * @param sequence the sequence number (0-99999)
     * @return a new reference number
     * @throws IllegalArgumentException if any component is invalid
     */
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

    /**
     * Returns the 3-character prefix of this reference number.
     *
     * @return the prefix (e.g., "ISS", "TRF", "RST")
     */
    public String prefix() {
        return value.substring(0, 3);
    }

    /**
     * Returns the year component of this reference number.
     *
     * @return the 4-digit year
     */
    public int year() {
        return Integer.parseInt(value.substring(4, 8));
    }

    /**
     * Returns the sequence number component of this reference number.
     *
     * @return the sequence number (0-99999)
     */
    public int sequence() {
        return Integer.parseInt(value.substring(9));
    }

    @Override
    public String toString() {
        return value;
    }
}
