package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;

import java.util.Objects;

/**
 * Target representing an individual equipment item being audited.
 *
 * <p>Equipment items are serialized assets that are tracked individually.
 * During an audit, each equipment item is verified for presence, condition,
 * and may undergo functional testing.
 *
 * @param equipmentItemId the unique identifier of the equipment item being audited
 */
public record EquipmentTarget(EquipmentItemId equipmentItemId) implements AuditedItemTarget {

    public EquipmentTarget {
        Objects.requireNonNull(equipmentItemId, "Equipment item ID cannot be null");
    }

    /**
     * Creates an equipment target for the given equipment item ID.
     *
     * @param equipmentItemId the equipment item ID
     * @return a new equipment target
     */
    public static EquipmentTarget of(EquipmentItemId equipmentItemId) {
        return new EquipmentTarget(equipmentItemId);
    }
}
