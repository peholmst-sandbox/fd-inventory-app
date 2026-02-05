package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;

import java.util.Objects;

/**
 * Target representing an individual equipment item being verified during an inventory check.
 *
 * <p>Equipment items are serialized assets that are tracked individually.
 * During an inventory check, each equipment item is verified for presence.
 *
 * @param equipmentItemId the unique identifier of the equipment item being verified
 */
public record EquipmentCheckTarget(EquipmentItemId equipmentItemId) implements CheckedItemTarget {

    public EquipmentCheckTarget {
        Objects.requireNonNull(equipmentItemId, "Equipment item ID cannot be null");
    }

    /**
     * Creates an equipment target for the given equipment item ID.
     *
     * @param equipmentItemId the equipment item ID
     * @return a new equipment target
     */
    public static EquipmentCheckTarget of(EquipmentItemId equipmentItemId) {
        return new EquipmentCheckTarget(equipmentItemId);
    }
}
