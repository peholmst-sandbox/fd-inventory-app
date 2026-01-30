package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record ManifestEntryId(UUID value) {
    public ManifestEntryId {
        Objects.requireNonNull(value, "Manifest entry ID cannot be null");
    }

    public static ManifestEntryId generate() {
        return new ManifestEntryId(UUID.randomUUID());
    }

    public static ManifestEntryId of(UUID value) {
        return new ManifestEntryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
