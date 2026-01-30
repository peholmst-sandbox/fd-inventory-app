package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record EquipmentTypeId(UUID value) {
    public EquipmentTypeId {
        Objects.requireNonNull(value, "Equipment type ID cannot be null");
    }

    public static EquipmentTypeId generate() {
        return new EquipmentTypeId(UUID.randomUUID());
    }

    public static EquipmentTypeId of(UUID value) {
        return new EquipmentTypeId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
