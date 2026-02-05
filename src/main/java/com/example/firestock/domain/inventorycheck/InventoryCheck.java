package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.CheckStatus;

import java.time.Instant;

/**
 * Sealed interface representing an inventory check (aggregate root).
 *
 * <p>An inventory check is a shift inventory verification performed by firefighters
 * on a specific apparatus. Unlike formal audits, inventory checks focus on quick
 * presence verification without detailed condition assessments.
 *
 * <p>The sealed interface enforces state-based behavior through its permitted implementations:
 * <ul>
 *   <li>{@link InProgressCheck} - Active check that can be modified, completed, or abandoned</li>
 *   <li>{@link CompletedCheck} - Terminal read-only state</li>
 *   <li>{@link AbandonedCheck} - Terminal state with optional resume capability</li>
 * </ul>
 *
 * <p>State transitions:
 * <pre>
 * InProgressCheck --complete()--> CompletedCheck
 * InProgressCheck --abandon()---> AbandonedCheck
 * AbandonedCheck  --resume()----> InProgressCheck (within 30-min window per BR-04)
 * </pre>
 */
public sealed interface InventoryCheck permits InProgressCheck, CompletedCheck, AbandonedCheck {

    /**
     * Returns the unique identifier of this check.
     *
     * @return the check ID
     */
    InventoryCheckId id();

    /**
     * Returns the apparatus being checked.
     *
     * @return the apparatus ID
     */
    ApparatusId apparatusId();

    /**
     * Returns the station where the apparatus is located.
     *
     * @return the station ID
     */
    StationId stationId();

    /**
     * Returns the user who initiated the check.
     *
     * @return the performer's user ID
     */
    UserId performedById();

    /**
     * Returns when the check was started.
     *
     * @return the start timestamp
     */
    Instant startedAt();

    /**
     * Returns the current progress of the check.
     *
     * @return the check progress
     */
    CheckProgress progress();

    /**
     * Returns the current status of the check.
     *
     * @return the check status
     */
    CheckStatus status();

    /**
     * Checks if this check is in a terminal state (completed or abandoned).
     *
     * @return true if the check cannot be modified
     */
    default boolean isTerminal() {
        return status() == CheckStatus.COMPLETED || status() == CheckStatus.ABANDONED;
    }

    /**
     * Checks if this check is active (in progress).
     *
     * @return true if the check can be modified
     */
    default boolean isActive() {
        return status() == CheckStatus.IN_PROGRESS;
    }
}
