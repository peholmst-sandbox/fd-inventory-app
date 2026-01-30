package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record InventoryCheckItemId(UUID value) {
    public InventoryCheckItemId {
        Objects.requireNonNull(value, "Inventory check item ID cannot be null");
    }

    public static InventoryCheckItemId generate() {
        return new InventoryCheckItemId(UUID.randomUUID());
    }

    public static InventoryCheckItemId of(UUID value) {
        return new InventoryCheckItemId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
