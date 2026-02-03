package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.numbers.Quantity;

import java.util.Objects;

/**
 * Value object comparing expected and found quantities for consumable audits.
 *
 * <p>During a formal audit, consumable stock levels are verified by comparing
 * the expected quantity (from the manifest) with the actual quantity found.
 *
 * @param expected the quantity expected per the manifest
 * @param found the actual quantity found during the audit
 */
public record QuantityComparison(Quantity expected, Quantity found) {

    public QuantityComparison {
        Objects.requireNonNull(expected, "Expected quantity cannot be null");
        Objects.requireNonNull(found, "Found quantity cannot be null");
    }

    /**
     * Creates a comparison where expected and found quantities match.
     *
     * @param quantity the matching quantity
     * @return a new comparison with equal expected and found values
     */
    public static QuantityComparison matching(Quantity quantity) {
        return new QuantityComparison(quantity, quantity);
    }

    /**
     * Creates a comparison from expected and found integer values.
     *
     * @param expected the expected quantity
     * @param found the found quantity
     * @return a new comparison
     */
    public static QuantityComparison of(int expected, int found) {
        return new QuantityComparison(Quantity.of(expected), Quantity.of(found));
    }

    /**
     * Checks if the found quantity matches the expected quantity.
     *
     * @return true if quantities are equal
     */
    public boolean isMatch() {
        return expected.equals(found);
    }

    /**
     * Checks if there is a shortage (found less than expected).
     *
     * @return true if found quantity is less than expected
     */
    public boolean isShortage() {
        return found.value().compareTo(expected.value()) < 0;
    }

    /**
     * Checks if there is a surplus (found more than expected).
     *
     * @return true if found quantity is greater than expected
     */
    public boolean isSurplus() {
        return found.value().compareTo(expected.value()) > 0;
    }

    /**
     * Calculates the difference between found and expected quantities.
     *
     * <p>A positive result indicates surplus, negative indicates shortage.
     *
     * @return the difference (found - expected) as a Quantity, or null if shortage
     */
    public Quantity difference() {
        if (isShortage()) {
            return expected.subtract(found);
        }
        return found.subtract(expected);
    }

    /**
     * Returns the shortage amount if there is a shortage.
     *
     * @return the shortage quantity, or zero if no shortage
     */
    public Quantity shortageAmount() {
        if (isShortage()) {
            return expected.subtract(found);
        }
        return Quantity.zero();
    }

    /**
     * Returns the surplus amount if there is a surplus.
     *
     * @return the surplus quantity, or zero if no surplus
     */
    public Quantity surplusAmount() {
        if (isSurplus()) {
            return found.subtract(expected);
        }
        return Quantity.zero();
    }
}
