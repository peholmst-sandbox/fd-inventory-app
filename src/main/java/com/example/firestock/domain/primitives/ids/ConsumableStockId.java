package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record ConsumableStockId(UUID value) {
    public ConsumableStockId {
        Objects.requireNonNull(value, "Consumable stock ID cannot be null");
    }

    public static ConsumableStockId generate() {
        return new ConsumableStockId(UUID.randomUUID());
    }

    public static ConsumableStockId of(UUID value) {
        return new ConsumableStockId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
