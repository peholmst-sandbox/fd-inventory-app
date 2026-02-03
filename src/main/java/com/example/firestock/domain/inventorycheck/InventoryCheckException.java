package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;

/**
 * Sealed exception hierarchy for inventory check domain errors.
 *
 * <p>Using a sealed class hierarchy allows exhaustive pattern matching on
 * exception types and ensures all inventory check-related exceptions are explicitly defined.
 */
public abstract sealed class InventoryCheckException extends RuntimeException {

    protected InventoryCheckException(String message) {
        super(message);
    }

    protected InventoryCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when attempting to complete a check that has unverified items.
     *
     * <p>Per BR-02, all items must be verified before completion.
     */
    public static final class IncompleteCheckException extends InventoryCheckException {
        private final InventoryCheckId checkId;
        private final int remainingItems;

        public IncompleteCheckException(InventoryCheckId checkId, int remainingItems) {
            super("Cannot complete check " + checkId + ": " + remainingItems + " items have not been verified");
            this.checkId = checkId;
            this.remainingItems = remainingItems;
        }

        public InventoryCheckId checkId() {
            return checkId;
        }

        public int remainingItems() {
            return remainingItems;
        }
    }

    /**
     * Thrown when attempting to modify a completed check.
     */
    public static final class CheckAlreadyCompletedException extends InventoryCheckException {
        private final InventoryCheckId checkId;

        public CheckAlreadyCompletedException(InventoryCheckId checkId) {
            super("Check " + checkId + " is already completed and cannot be modified");
            this.checkId = checkId;
        }

        public InventoryCheckId checkId() {
            return checkId;
        }
    }

    /**
     * Thrown when attempting to modify an abandoned check.
     */
    public static final class CheckAlreadyAbandonedException extends InventoryCheckException {
        private final InventoryCheckId checkId;

        public CheckAlreadyAbandonedException(InventoryCheckId checkId) {
            super("Check " + checkId + " has been abandoned and cannot be modified");
            this.checkId = checkId;
        }

        public InventoryCheckId checkId() {
            return checkId;
        }
    }

    /**
     * Thrown when attempting to start a new check for an apparatus that already has
     * an active check in progress.
     *
     * <p>Per BR-01, only one active check per apparatus is allowed.
     */
    public static final class ActiveCheckExistsException extends InventoryCheckException {
        private final ApparatusId apparatusId;
        private final InventoryCheckId existingCheckId;

        public ActiveCheckExistsException(ApparatusId apparatusId, InventoryCheckId existingCheckId) {
            super("Apparatus " + apparatusId + " already has an active check: " + existingCheckId);
            this.apparatusId = apparatusId;
            this.existingCheckId = existingCheckId;
        }

        public ApparatusId apparatusId() {
            return apparatusId;
        }

        public InventoryCheckId existingCheckId() {
            return existingCheckId;
        }
    }

    /**
     * Thrown when a requested check cannot be found.
     */
    public static final class CheckNotFoundException extends InventoryCheckException {
        private final InventoryCheckId checkId;

        public CheckNotFoundException(InventoryCheckId checkId) {
            super("Check not found: " + checkId);
            this.checkId = checkId;
        }

        public InventoryCheckId checkId() {
            return checkId;
        }
    }

    /**
     * Thrown when attempting to verify an item that has already been verified
     * in the same check session.
     *
     * <p>Per BR-04, each item can only be verified once per check.
     */
    public static final class ItemAlreadyVerifiedException extends InventoryCheckException {
        private final InventoryCheckId checkId;
        private final CheckedItemTarget target;

        public ItemAlreadyVerifiedException(InventoryCheckId checkId, CheckedItemTarget target) {
            super("Item has already been verified in this check: " + target);
            this.checkId = checkId;
            this.target = target;
        }

        public InventoryCheckId checkId() {
            return checkId;
        }

        public CheckedItemTarget target() {
            return target;
        }
    }

    /**
     * Thrown when attempting to resume a check outside the resume window.
     *
     * <p>Per BR-04, abandoned checks can only be resumed within 30 minutes.
     */
    public static final class ResumeWindowExpiredException extends InventoryCheckException {
        private final InventoryCheckId checkId;

        public ResumeWindowExpiredException(InventoryCheckId checkId) {
            super("Resume window has expired for check " + checkId + ". Cannot resume after 30 minutes.");
            this.checkId = checkId;
        }

        public InventoryCheckId checkId() {
            return checkId;
        }
    }
}
