package com.example.firestock.domain.issue;

/**
 * Sealed interface representing the target of an issue.
 *
 * <p>An issue targets either a piece of equipment, a consumable stock entry,
 * or an apparatus (for apparatus-level issues with no specific item).
 * This sealed interface enforces the XOR constraint at the type level.
 *
 * <p>Permitted implementations:
 * <ul>
 *   <li>{@link EquipmentIssueTarget} - For issues with individual equipment items</li>
 *   <li>{@link ConsumableIssueTarget} - For issues with consumable stock entries</li>
 *   <li>{@link ApparatusIssueTarget} - For apparatus-level issues with no specific item</li>
 * </ul>
 */
public sealed interface IssueTarget permits EquipmentIssueTarget, ConsumableIssueTarget, ApparatusIssueTarget {

    /**
     * Checks if this target represents an equipment item.
     *
     * @return true if this is an equipment target
     */
    default boolean isEquipment() {
        return this instanceof EquipmentIssueTarget;
    }

    /**
     * Checks if this target represents a consumable stock entry.
     *
     * @return true if this is a consumable target
     */
    default boolean isConsumable() {
        return this instanceof ConsumableIssueTarget;
    }

    /**
     * Checks if this target represents an apparatus-level issue.
     *
     * @return true if this is an apparatus-level target
     */
    default boolean isApparatusLevel() {
        return this instanceof ApparatusIssueTarget;
    }
}
