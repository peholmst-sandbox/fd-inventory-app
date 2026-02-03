package com.example.firestock.domain.issue;

import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.jooq.enums.IssueStatus;

/**
 * Sealed exception hierarchy for issue domain errors.
 *
 * <p>Using a sealed class hierarchy allows exhaustive pattern matching on
 * exception types and ensures all issue-related exceptions are explicitly defined.
 */
public abstract sealed class IssueException extends RuntimeException {

    protected IssueException(String message) {
        super(message);
    }

    protected IssueException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when a requested issue cannot be found.
     */
    public static final class IssueNotFoundException extends IssueException {
        private final IssueId issueId;

        public IssueNotFoundException(IssueId issueId) {
            super("Issue not found: " + issueId);
            this.issueId = issueId;
        }

        public IssueId issueId() {
            return issueId;
        }
    }

    /**
     * Thrown when attempting to perform a state transition that is not allowed
     * from the current state.
     */
    public static final class InvalidStateTransitionException extends IssueException {
        private final IssueId issueId;
        private final IssueStatus currentStatus;
        private final IssueStatus targetStatus;

        public InvalidStateTransitionException(IssueId issueId, IssueStatus currentStatus, IssueStatus targetStatus) {
            super("Cannot transition issue " + issueId + " from " + currentStatus + " to " + targetStatus);
            this.issueId = issueId;
            this.currentStatus = currentStatus;
            this.targetStatus = targetStatus;
        }

        public IssueId issueId() {
            return issueId;
        }

        public IssueStatus currentStatus() {
            return currentStatus;
        }

        public IssueStatus targetStatus() {
            return targetStatus;
        }
    }

    /**
     * Thrown when attempting to modify an issue that is in a terminal state
     * (RESOLVED or CLOSED).
     */
    public static final class IssueAlreadyClosedException extends IssueException {
        private final IssueId issueId;
        private final IssueStatus status;

        public IssueAlreadyClosedException(IssueId issueId, IssueStatus status) {
            super("Issue " + issueId + " is already " + status + " and cannot be modified");
            this.issueId = issueId;
            this.status = status;
        }

        public IssueId issueId() {
            return issueId;
        }

        public IssueStatus status() {
            return status;
        }
    }

    /**
     * Thrown when attempting to resolve an issue without providing resolution notes.
     *
     * <p>Per BR-07, resolution notes are required when resolving an issue.
     */
    public static final class ResolutionNotesRequiredException extends IssueException {
        private final IssueId issueId;

        public ResolutionNotesRequiredException(IssueId issueId) {
            super("Resolution notes are required when resolving issue " + issueId);
            this.issueId = issueId;
        }

        public IssueId issueId() {
            return issueId;
        }
    }
}
