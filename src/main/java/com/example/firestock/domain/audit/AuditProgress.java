package com.example.firestock.domain.audit;

import java.util.Objects;

/**
 * Value object representing the progress of a formal audit.
 *
 * <p>Tracks the total number of items to audit, how many have been audited,
 * how many issues were found, and how many unexpected items were discovered.
 *
 * @param totalItems the total number of items expected on the manifest
 * @param auditedCount the number of items that have been audited
 * @param issuesFoundCount the number of items with issues (MISSING, DAMAGED, etc.)
 * @param unexpectedItemsCount the number of items found that weren't on the manifest
 */
public record AuditProgress(
        int totalItems,
        int auditedCount,
        int issuesFoundCount,
        int unexpectedItemsCount
) {
    public AuditProgress {
        if (totalItems < 0) {
            throw new IllegalArgumentException("Total items cannot be negative: " + totalItems);
        }
        if (auditedCount < 0) {
            throw new IllegalArgumentException("Audited count cannot be negative: " + auditedCount);
        }
        if (auditedCount > totalItems) {
            throw new IllegalArgumentException(
                    "Audited count (" + auditedCount + ") cannot exceed total items (" + totalItems + ")");
        }
        if (issuesFoundCount < 0) {
            throw new IllegalArgumentException("Issues found count cannot be negative: " + issuesFoundCount);
        }
        if (unexpectedItemsCount < 0) {
            throw new IllegalArgumentException("Unexpected items count cannot be negative: " + unexpectedItemsCount);
        }
    }

    /**
     * Creates initial progress for an audit with the given total items.
     *
     * @param totalItems the total number of items to audit
     * @return a new progress with zero audited items and issues
     */
    public static AuditProgress initial(int totalItems) {
        return new AuditProgress(totalItems, 0, 0, 0);
    }

    /**
     * Creates an empty progress (no items to audit).
     *
     * @return a new progress with all counts at zero
     */
    public static AuditProgress empty() {
        return new AuditProgress(0, 0, 0, 0);
    }

    /**
     * Calculates the progress as a percentage (0-100).
     *
     * @return the percentage of items audited, or 100 if there are no items
     */
    public int progressPercentage() {
        if (totalItems == 0) {
            return 100;
        }
        return (auditedCount * 100) / totalItems;
    }

    /**
     * Checks if all items have been audited.
     *
     * <p>This is required for audit completion per BR-03.
     *
     * @return true if all items have been audited
     */
    public boolean isAllAudited() {
        return auditedCount == totalItems;
    }

    /**
     * Returns the number of items remaining to audit.
     *
     * @return the count of unaudited items
     */
    public int remainingItems() {
        return totalItems - auditedCount;
    }

    /**
     * Returns true if any issues have been found during the audit.
     *
     * @return true if at least one issue was found
     */
    public boolean hasIssues() {
        return issuesFoundCount > 0;
    }

    /**
     * Returns true if any unexpected items were found during the audit.
     *
     * @return true if at least one unexpected item was found
     */
    public boolean hasUnexpectedItems() {
        return unexpectedItemsCount > 0;
    }

    /**
     * Creates a new progress with one more item audited.
     *
     * @param hasIssue whether the audited item has an issue
     * @return a new progress with updated counts
     */
    public AuditProgress withItemAudited(boolean hasIssue) {
        return new AuditProgress(
                totalItems,
                auditedCount + 1,
                hasIssue ? issuesFoundCount + 1 : issuesFoundCount,
                unexpectedItemsCount
        );
    }

    /**
     * Creates a new progress with one more unexpected item recorded.
     *
     * @param hasIssue whether the unexpected item has an issue
     * @return a new progress with updated counts
     */
    public AuditProgress withUnexpectedItem(boolean hasIssue) {
        return new AuditProgress(
                totalItems,
                auditedCount,
                hasIssue ? issuesFoundCount + 1 : issuesFoundCount,
                unexpectedItemsCount + 1
        );
    }

    /**
     * Creates a new progress with an updated total items count.
     *
     * @param newTotalItems the new total items count
     * @return a new progress with the updated total
     */
    public AuditProgress withTotalItems(int newTotalItems) {
        return new AuditProgress(newTotalItems, auditedCount, issuesFoundCount, unexpectedItemsCount);
    }
}
