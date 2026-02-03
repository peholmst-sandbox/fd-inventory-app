package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;

/**
 * Sealed exception hierarchy for audit domain errors.
 *
 * <p>Using a sealed class hierarchy allows exhaustive pattern matching on
 * exception types and ensures all audit-related exceptions are explicitly defined.
 */
public abstract sealed class AuditException extends RuntimeException {

    protected AuditException(String message) {
        super(message);
    }

    protected AuditException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when attempting to complete an audit that has unaudited items.
     *
     * <p>Per BR-03, all items must be audited before completion.
     */
    public static final class IncompleteAuditException extends AuditException {
        private final FormalAuditId auditId;
        private final int remainingItems;

        public IncompleteAuditException(FormalAuditId auditId, int remainingItems) {
            super("Cannot complete audit " + auditId + ": " + remainingItems + " items have not been audited");
            this.auditId = auditId;
            this.remainingItems = remainingItems;
        }

        public FormalAuditId auditId() {
            return auditId;
        }

        public int remainingItems() {
            return remainingItems;
        }
    }

    /**
     * Thrown when attempting to modify a completed audit.
     *
     * <p>Per BR-07, completed audits are read-only.
     */
    public static final class AuditAlreadyCompletedException extends AuditException {
        private final FormalAuditId auditId;

        public AuditAlreadyCompletedException(FormalAuditId auditId) {
            super("Audit " + auditId + " is already completed and cannot be modified");
            this.auditId = auditId;
        }

        public FormalAuditId auditId() {
            return auditId;
        }
    }

    /**
     * Thrown when attempting to modify an abandoned audit.
     */
    public static final class AuditAlreadyAbandonedException extends AuditException {
        private final FormalAuditId auditId;

        public AuditAlreadyAbandonedException(FormalAuditId auditId) {
            super("Audit " + auditId + " has been abandoned and cannot be modified");
            this.auditId = auditId;
        }

        public FormalAuditId auditId() {
            return auditId;
        }
    }

    /**
     * Thrown when attempting to start a new audit for an apparatus that already has
     * an active audit in progress.
     *
     * <p>Per BR-02, only one active audit per apparatus is allowed.
     */
    public static final class ActiveAuditExistsException extends AuditException {
        private final ApparatusId apparatusId;
        private final FormalAuditId existingAuditId;

        public ActiveAuditExistsException(ApparatusId apparatusId, FormalAuditId existingAuditId) {
            super("Apparatus " + apparatusId + " already has an active audit: " + existingAuditId);
            this.apparatusId = apparatusId;
            this.existingAuditId = existingAuditId;
        }

        public ApparatusId apparatusId() {
            return apparatusId;
        }

        public FormalAuditId existingAuditId() {
            return existingAuditId;
        }
    }

    /**
     * Thrown when attempting to resume an audit that is not paused.
     */
    public static final class AuditNotPausedException extends AuditException {
        private final FormalAuditId auditId;

        public AuditNotPausedException(FormalAuditId auditId) {
            super("Audit " + auditId + " is not paused and cannot be resumed");
            this.auditId = auditId;
        }

        public FormalAuditId auditId() {
            return auditId;
        }
    }

    /**
     * Thrown when attempting to pause an audit that is already paused.
     */
    public static final class AuditAlreadyPausedException extends AuditException {
        private final FormalAuditId auditId;

        public AuditAlreadyPausedException(FormalAuditId auditId) {
            super("Audit " + auditId + " is already paused");
            this.auditId = auditId;
        }

        public FormalAuditId auditId() {
            return auditId;
        }
    }

    /**
     * Thrown when a requested audit cannot be found.
     */
    public static final class AuditNotFoundException extends AuditException {
        private final FormalAuditId auditId;

        public AuditNotFoundException(FormalAuditId auditId) {
            super("Audit not found: " + auditId);
            this.auditId = auditId;
        }

        public FormalAuditId auditId() {
            return auditId;
        }
    }
}
