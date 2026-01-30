package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record CompartmentId(UUID value) {
    public CompartmentId {
        Objects.requireNonNull(value, "Compartment ID cannot be null");
    }

    public static CompartmentId generate() {
        return new CompartmentId(UUID.randomUUID());
    }

    public static CompartmentId of(UUID value) {
        return new CompartmentId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
