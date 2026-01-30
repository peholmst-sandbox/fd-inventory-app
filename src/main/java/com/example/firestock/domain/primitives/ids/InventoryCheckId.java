package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record InventoryCheckId(UUID value) {
    public InventoryCheckId {
        Objects.requireNonNull(value, "Inventory check ID cannot be null");
    }

    public static InventoryCheckId generate() {
        return new InventoryCheckId(UUID.randomUUID());
    }

    public static InventoryCheckId of(UUID value) {
        return new InventoryCheckId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
