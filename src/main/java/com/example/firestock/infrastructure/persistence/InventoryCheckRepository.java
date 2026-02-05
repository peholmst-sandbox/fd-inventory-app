package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.inventorycheck.AbandonedCheck;
import com.example.firestock.domain.inventorycheck.CompletedCheck;
import com.example.firestock.domain.inventorycheck.InProgressCheck;
import com.example.firestock.domain.inventorycheck.InventoryCheck;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.infrastructure.persistence.mapper.InventoryCheckMapper;
import com.example.firestock.jooq.enums.CheckStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK;

/**
 * Repository for {@link InventoryCheck} aggregate persistence.
 *
 * <p>Handles storage and retrieval of inventory checks across all states
 * (in-progress, completed, abandoned). The repository works with domain types and
 * handles the sealed interface hierarchy by mapping based on the status field.
 *
 * <h3>Aggregate Boundaries</h3>
 * <p>InventoryCheck is an aggregate root. The repository manages:
 * <ul>
 *   <li>The check entity itself (all states)</li>
 *   <li>Transactional consistency for state transitions</li>
 * </ul>
 *
 * <p>InventoryCheckItem is handled by {@link InventoryCheckItemRepository} as items may be
 * added incrementally during a check session.
 *
 * <h3>State Handling</h3>
 * <p>Methods like {@link #save(InventoryCheck)} accept any check state and persist
 * appropriately. The sealed interface hierarchy ensures type safety:
 * <ul>
 *   <li>{@link InProgressCheck} - active, mutable check</li>
 *   <li>{@link CompletedCheck} - terminal, read-only check</li>
 *   <li>{@link AbandonedCheck} - terminal, resumable within window</li>
 * </ul>
 */
@Repository
public class InventoryCheckRepository {

    private final DSLContext create;
    private final InventoryCheckMapper mapper;

    public InventoryCheckRepository(DSLContext create, InventoryCheckMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves an inventory check (insert or update based on existence).
     *
     * <p>This method handles all check states. The implementation:
     * <ul>
     *   <li>Inserts if the check doesn't exist</li>
     *   <li>Updates if the check exists</li>
     *   <li>Persists state-specific fields appropriately</li>
     * </ul>
     *
     * @param check the check to save (any state)
     * @return the saved check
     */
    public InventoryCheck save(InventoryCheck check) {
        var record = create.newRecord(INVENTORY_CHECK);
        mapper.updateRecord(record, check);

        if (existsById(check.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return check;
    }

    /**
     * Finds an inventory check by its ID.
     *
     * <p>Returns the check in its current state (InProgressCheck, CompletedCheck,
     * or AbandonedCheck).
     *
     * @param id the check ID
     * @return the check, or empty if not found
     */
    public Optional<InventoryCheck> findById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if a check with the given ID exists.
     *
     * @param id the check ID
     * @return true if the check exists
     */
    public boolean existsById(InventoryCheckId id) {
        return create.fetchExists(
                create.selectFrom(INVENTORY_CHECK)
                        .where(INVENTORY_CHECK.ID.eq(id))
        );
    }

    /**
     * Deletes an inventory check by its ID.
     *
     * <p><b>Warning:</b> This permanently removes the check and should only be used
     * for cleanup of test data or administrative purposes. Completed checks should
     * generally be retained for historical records.
     *
     * @param id the check ID to delete
     */
    public void deleteById(InventoryCheckId id) {
        create.deleteFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.ID.eq(id))
                .execute();
    }

    // ========================================================================
    // State-Specific Queries
    // ========================================================================

    /**
     * Finds an in-progress check by its ID.
     *
     * <p>Use this when you specifically need an in-progress check for mutation.
     *
     * @param id the check ID
     * @return the in-progress check, or empty if not found or not in-progress
     */
    public Optional<InProgressCheck> findInProgressById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.ID.eq(id))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InProgressCheck.class::isInstance)
                .map(InProgressCheck.class::cast);
    }

    /**
     * Finds a completed check by its ID.
     *
     * @param id the check ID
     * @return the completed check, or empty if not found or not completed
     */
    public Optional<CompletedCheck> findCompletedById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.ID.eq(id))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(CompletedCheck.class::isInstance)
                .map(CompletedCheck.class::cast);
    }

    /**
     * Finds an abandoned check by its ID.
     *
     * @param id the check ID
     * @return the abandoned check, or empty if not found or not abandoned
     */
    public Optional<AbandonedCheck> findAbandonedById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.ID.eq(id))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.ABANDONED))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(AbandonedCheck.class::isInstance)
                .map(AbandonedCheck.class::cast);
    }

    // ========================================================================
    // Business Rule Queries
    // ========================================================================

    /**
     * Finds an active (in-progress) check for an apparatus.
     *
     * <p>This is used to enforce BR-01: Only one active check per apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the active check, or empty if no active check exists
     */
    public Optional<InProgressCheck> findActiveByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InProgressCheck.class::isInstance)
                .map(InProgressCheck.class::cast);
    }

    /**
     * Checks if an active check exists for the given apparatus.
     *
     * <p>Convenience method for BR-01 enforcement.
     *
     * @param apparatusId the apparatus ID
     * @return true if an active check exists
     */
    public boolean hasActiveCheckForApparatus(ApparatusId apparatusId) {
        return create.fetchExists(
                create.selectFrom(INVENTORY_CHECK)
                        .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
                        .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
        );
    }

    /**
     * Finds resumable abandoned checks for a user.
     *
     * <p>Per BR-04, abandoned checks can be resumed within 30 minutes.
     *
     * @param performerId the user ID of the performer
     * @param now the current timestamp for resume window calculation
     * @return list of resumable abandoned checks
     */
    public List<AbandonedCheck> findResumable(UserId performerId, Instant now) {
        var resumeWindowStart = now.minus(Duration.ofMinutes(InProgressCheck.RESUME_WINDOW_MINUTES));

        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.PERFORMED_BY_ID.eq(performerId))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.ABANDONED))
                .and(INVENTORY_CHECK.ABANDONED_AT.ge(resumeWindowStart))
                .orderBy(INVENTORY_CHECK.ABANDONED_AT.desc())
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(AbandonedCheck.class::isInstance)
                .map(AbandonedCheck.class::cast)
                .toList();
    }

    // ========================================================================
    // List Queries
    // ========================================================================

    /**
     * Finds all checks for a specific apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return list of all checks (any state) for the apparatus, ordered by startedAt descending
     */
    public List<InventoryCheck> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
                .orderBy(INVENTORY_CHECK.STARTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all checks for a specific station.
     *
     * @param stationId the station ID
     * @return list of all checks for the station, ordered by startedAt descending
     */
    public List<InventoryCheck> findByStationId(StationId stationId) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.STATION_ID.eq(stationId))
                .orderBy(INVENTORY_CHECK.STARTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all in-progress checks.
     *
     * <p>Useful for administrative dashboards and stale check detection (BR-03).
     *
     * @return list of all in-progress checks
     */
    public List<InProgressCheck> findAllInProgress() {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InProgressCheck.class::isInstance)
                .map(InProgressCheck.class::cast)
                .toList();
    }

    /**
     * Finds all in-progress checks for a specific performer.
     *
     * @param performerId the user ID of the performer
     * @return list of in-progress checks by the performer
     */
    public List<InProgressCheck> findInProgressByPerformerId(UserId performerId) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.PERFORMED_BY_ID.eq(performerId))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InProgressCheck.class::isInstance)
                .map(InProgressCheck.class::cast)
                .toList();
    }

    /**
     * Finds completed checks for an apparatus within a date range.
     *
     * @param apparatusId the apparatus ID
     * @param fromInclusive start of date range (inclusive)
     * @param toExclusive end of date range (exclusive)
     * @return list of completed checks in the date range
     */
    public List<CompletedCheck> findCompletedByApparatusIdAndDateRange(
            ApparatusId apparatusId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
                .and(INVENTORY_CHECK.COMPLETED_AT.ge(fromInclusive))
                .and(INVENTORY_CHECK.COMPLETED_AT.lt(toExclusive))
                .orderBy(INVENTORY_CHECK.COMPLETED_AT.desc())
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(CompletedCheck.class::isInstance)
                .map(CompletedCheck.class::cast)
                .toList();
    }

    /**
     * Finds completed checks for a station within a date range.
     *
     * @param stationId the station ID
     * @param fromInclusive start of date range (inclusive)
     * @param toExclusive end of date range (exclusive)
     * @return list of completed checks in the date range
     */
    public List<CompletedCheck> findCompletedByStationIdAndDateRange(
            StationId stationId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return create.selectFrom(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.STATION_ID.eq(stationId))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
                .and(INVENTORY_CHECK.COMPLETED_AT.ge(fromInclusive))
                .and(INVENTORY_CHECK.COMPLETED_AT.lt(toExclusive))
                .orderBy(INVENTORY_CHECK.COMPLETED_AT.desc())
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(CompletedCheck.class::isInstance)
                .map(CompletedCheck.class::cast)
                .toList();
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all checks for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of checks
     */
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts in-progress checks (useful for dashboard metrics).
     *
     * @return the count of in-progress checks
     */
    public long countInProgress() {
        return create.selectCount()
                .from(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts completed checks for a station within a date range.
     *
     * @param stationId the station ID
     * @param fromInclusive start of date range (inclusive)
     * @param toExclusive end of date range (exclusive)
     * @return the count of completed checks
     */
    public long countCompletedByStationIdAndDateRange(
            StationId stationId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return create.selectCount()
                .from(INVENTORY_CHECK)
                .where(INVENTORY_CHECK.STATION_ID.eq(stationId))
                .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
                .and(INVENTORY_CHECK.COMPLETED_AT.ge(fromInclusive))
                .and(INVENTORY_CHECK.COMPLETED_AT.lt(toExclusive))
                .fetchOne(0, Long.class);
    }
}
