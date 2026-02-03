package com.example.firestock.domain.inventorycheck;

/**
 * Value object representing the progress of an inventory check.
 *
 * <p>Tracks the total number of items to verify, how many have been verified,
 * and how many issues were found.
 *
 * @param totalItems the total number of items expected on the apparatus
 * @param verifiedCount the number of items that have been verified
 * @param issuesFoundCount the number of items with issues (MISSING, DAMAGED, etc.)
 */
public record CheckProgress(
        int totalItems,
        int verifiedCount,
        int issuesFoundCount
) {
    public CheckProgress {
        if (totalItems < 0) {
            throw new IllegalArgumentException("Total items cannot be negative: " + totalItems);
        }
        if (verifiedCount < 0) {
            throw new IllegalArgumentException("Verified count cannot be negative: " + verifiedCount);
        }
        if (verifiedCount > totalItems) {
            throw new IllegalArgumentException(
                    "Verified count (" + verifiedCount + ") cannot exceed total items (" + totalItems + ")");
        }
        if (issuesFoundCount < 0) {
            throw new IllegalArgumentException("Issues found count cannot be negative: " + issuesFoundCount);
        }
    }

    /**
     * Creates initial progress for a check with the given total items.
     *
     * @param totalItems the total number of items to verify
     * @return a new progress with zero verified items and issues
     */
    public static CheckProgress initial(int totalItems) {
        return new CheckProgress(totalItems, 0, 0);
    }

    /**
     * Creates an empty progress (no items to verify).
     *
     * @return a new progress with all counts at zero
     */
    public static CheckProgress empty() {
        return new CheckProgress(0, 0, 0);
    }

    /**
     * Calculates the progress as a percentage (0-100).
     *
     * @return the percentage of items verified, or 100 if there are no items
     */
    public int progressPercentage() {
        if (totalItems == 0) {
            return 100;
        }
        return (verifiedCount * 100) / totalItems;
    }

    /**
     * Checks if all items have been verified.
     *
     * <p>This is required for check completion per BR-02.
     *
     * @return true if all items have been verified
     */
    public boolean isAllVerified() {
        return verifiedCount == totalItems;
    }

    /**
     * Returns the number of items remaining to verify.
     *
     * @return the count of unverified items
     */
    public int remainingItems() {
        return totalItems - verifiedCount;
    }

    /**
     * Returns true if any issues have been found during the check.
     *
     * @return true if at least one issue was found
     */
    public boolean hasIssues() {
        return issuesFoundCount > 0;
    }

    /**
     * Creates a new progress with one more item verified.
     *
     * @param hasIssue whether the verified item has an issue
     * @return a new progress with updated counts
     */
    public CheckProgress withItemVerified(boolean hasIssue) {
        return new CheckProgress(
                totalItems,
                verifiedCount + 1,
                hasIssue ? issuesFoundCount + 1 : issuesFoundCount
        );
    }

    /**
     * Creates a new progress with an updated total items count.
     *
     * @param newTotalItems the new total items count
     * @return a new progress with the updated total
     */
    public CheckProgress withTotalItems(int newTotalItems) {
        return new CheckProgress(newTotalItems, verifiedCount, issuesFoundCount);
    }
}
