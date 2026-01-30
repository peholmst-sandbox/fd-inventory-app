package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record RestockRecordId(UUID value) {
    public RestockRecordId {
        Objects.requireNonNull(value, "Restock record ID cannot be null");
    }

    public static RestockRecordId generate() {
        return new RestockRecordId(UUID.randomUUID());
    }

    public static RestockRecordId of(UUID value) {
        return new RestockRecordId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
