package com.example.firestock.domain.audit;

/**
 * Status of an individual item within a formal audit.
 *
 * <p>Each item in an audit receives a status indicating the verification result.
 * Certain statuses automatically trigger issue creation per BR-05.
 */
public enum AuditItemStatus {
    /**
     * Item has been verified and is in acceptable condition.
     * Does not require issue creation.
     */
    VERIFIED,

    /**
     * Item was not found at its expected location.
     * Requires issue creation per BR-05.
     */
    MISSING,

    /**
     * Item is present but has physical damage.
     * Requires issue creation per BR-05.
     */
    DAMAGED,

    /**
     * Item failed its functional inspection test.
     * Requires issue creation per BR-05.
     */
    FAILED_INSPECTION,

    /**
     * Item has expired or is past its certification date.
     * Requires issue creation per BR-05.
     */
    EXPIRED,

    /**
     * Item has not yet been audited. Initial state for items
     * added to an audit.
     */
    NOT_AUDITED;

    /**
     * Determines if this status requires automatic issue creation per BR-05.
     *
     * @return true if an issue should be created for this status
     */
    public boolean requiresIssue() {
        return this == MISSING || this == DAMAGED || this == FAILED_INSPECTION || this == EXPIRED;
    }
}
