package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.CheckStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A completed inventory check in a read-only terminal state.
 *
 * <p>Completed checks cannot be modified. This record has no mutation methods
 * to enforce this constraint at compile time.
 *
 * <p>Completed checks preserve all findings and progress information for
 * historical reference and reporting.
 *
 * @param id the unique identifier of this check
 * @param apparatusId the apparatus that was checked
 * @param stationId the station where the apparatus is located
 * @param performedById the user who performed the check
 * @param startedAt when the check was started
 * @param progress the final check progress (all items verified)
 * @param completedAt when the check was completed
 */
public record CompletedCheck(
        InventoryCheckId id,
        ApparatusId apparatusId,
        StationId stationId,
        UserId performedById,
        Instant startedAt,
        CheckProgress progress,
        Instant completedAt
) implements InventoryCheck {

    public CompletedCheck {
        Objects.requireNonNull(id, "Check ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
        Objects.requireNonNull(performedById, "Performer ID cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
        Objects.requireNonNull(progress, "Progress cannot be null");
        Objects.requireNonNull(completedAt, "Completed at cannot be null");

        if (!progress.isAllVerified()) {
            throw new IllegalArgumentException(
                    "Completed check must have all items verified, but " +
                    progress.remainingItems() + " items remain");
        }

        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Completed at cannot be before started at");
        }
    }

    @Override
    public CheckStatus status() {
        return CheckStatus.COMPLETED;
    }

    /**
     * Calculates the duration of the check from start to completion.
     *
     * @return the duration of the check
     */
    public Duration duration() {
        return Duration.between(startedAt, completedAt);
    }

    /**
     * Checks if the check found any issues.
     *
     * @return true if any items had issues
     */
    public boolean foundIssues() {
        return progress.hasIssues();
    }

    /**
     * Returns the total number of items verified.
     *
     * @return the count of verified items
     */
    public int totalItemsVerified() {
        return progress.totalItems();
    }

    /**
     * Returns the number of issues found during the check.
     *
     * @return the count of items with issues
     */
    public int issueCount() {
        return progress.issuesFoundCount();
    }
}
