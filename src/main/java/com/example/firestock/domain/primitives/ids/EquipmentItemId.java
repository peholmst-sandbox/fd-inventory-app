package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record EquipmentItemId(UUID value) {
    public EquipmentItemId {
        Objects.requireNonNull(value, "Equipment item ID cannot be null");
    }

    public static EquipmentItemId generate() {
        return new EquipmentItemId(UUID.randomUUID());
    }

    public static EquipmentItemId of(UUID value) {
        return new EquipmentItemId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
