package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record TransferRecordId(UUID value) {
    public TransferRecordId {
        Objects.requireNonNull(value, "Transfer record ID cannot be null");
    }

    public static TransferRecordId generate() {
        return new TransferRecordId(UUID.randomUUID());
    }

    public static TransferRecordId of(UUID value) {
        return new TransferRecordId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
