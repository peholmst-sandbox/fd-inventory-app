package com.example.firestock.domain.inventorycheck;

/**
 * Sealed interface representing the target of an inventory check item.
 *
 * <p>A checked item targets either a piece of equipment OR a consumable stock entry,
 * but never both. This sealed interface enforces this XOR constraint at the type level.
 *
 * <p>Permitted implementations:
 * <ul>
 *   <li>{@link EquipmentCheckTarget} - For verifying individual equipment items</li>
 *   <li>{@link ConsumableCheckTarget} - For verifying consumable stock entries</li>
 * </ul>
 */
public sealed interface CheckedItemTarget permits EquipmentCheckTarget, ConsumableCheckTarget {

    /**
     * Checks if this target represents an equipment item.
     *
     * @return true if this is an equipment target
     */
    default boolean isEquipment() {
        return this instanceof EquipmentCheckTarget;
    }

    /**
     * Checks if this target represents a consumable stock entry.
     *
     * @return true if this is a consumable target
     */
    default boolean isConsumable() {
        return this instanceof ConsumableCheckTarget;
    }
}
