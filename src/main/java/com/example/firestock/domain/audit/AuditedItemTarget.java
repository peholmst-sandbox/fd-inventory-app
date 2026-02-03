package com.example.firestock.domain.audit;

/**
 * Sealed interface representing the target of an audit item.
 *
 * <p>An audited item targets either a piece of equipment OR a consumable stock entry,
 * but never both. This sealed interface enforces this XOR constraint at the type level.
 *
 * <p>Permitted implementations:
 * <ul>
 *   <li>{@link EquipmentTarget} - For auditing individual equipment items</li>
 *   <li>{@link ConsumableTarget} - For auditing consumable stock entries</li>
 * </ul>
 */
public sealed interface AuditedItemTarget permits EquipmentTarget, ConsumableTarget {

    /**
     * Checks if this target represents an equipment item.
     *
     * @return true if this is an equipment target
     */
    default boolean isEquipment() {
        return this instanceof EquipmentTarget;
    }

    /**
     * Checks if this target represents a consumable stock entry.
     *
     * @return true if this is a consumable target
     */
    default boolean isConsumable() {
        return this instanceof ConsumableTarget;
    }
}
