package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a manifest entry.
 *
 * <p>A manifest entry defines a required equipment type and quantity for a specific
 * apparatus and compartment. The collection of manifest entries for an apparatus
 * constitutes its equipment manifest - the expected inventory that should be present
 * and verified during checks.
 *
 * @param value the UUID value, must not be null
 */
public record ManifestEntryId(UUID value) {
    public ManifestEntryId {
        Objects.requireNonNull(value, "Manifest entry ID cannot be null");
    }

    /**
     * Generates a new manifest entry ID with a random UUID.
     *
     * @return a new manifest entry ID
     */
    public static ManifestEntryId generate() {
        return new ManifestEntryId(UUID.randomUUID());
    }

    /**
     * Creates a manifest entry ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a manifest entry ID wrapping the given UUID
     */
    public static ManifestEntryId of(UUID value) {
        return new ManifestEntryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
