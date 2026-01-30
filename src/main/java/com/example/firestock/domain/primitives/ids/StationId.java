package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record StationId(UUID value) {
    public StationId {
        Objects.requireNonNull(value, "Station ID cannot be null");
    }

    public static StationId generate() {
        return new StationId(UUID.randomUUID());
    }

    public static StationId of(UUID value) {
        return new StationId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
