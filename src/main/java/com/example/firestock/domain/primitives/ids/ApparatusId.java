package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record ApparatusId(UUID value) {
    public ApparatusId {
        Objects.requireNonNull(value, "Apparatus ID cannot be null");
    }

    public static ApparatusId generate() {
        return new ApparatusId(UUID.randomUUID());
    }

    public static ApparatusId of(UUID value) {
        return new ApparatusId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
